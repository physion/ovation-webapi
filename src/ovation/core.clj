(ns ovation.core
  (:require [ovation.couch :as couch]
            [ovation.transform.read :as tr]
            [ovation.transform.write :as tw]
            [ovation.auth :as auth]
            [slingshot.slingshot :refer [throw+ try+]]
            [ovation.util :as util]
            [ovation.constants :as k]))



;; QUERY
(defn filter-trashed
  "Removes entity documents with a non-nil trash_info from seq"
  [entities include_trashed]
  (filter #(or include_trashed (nil? (:trash_info %))) entities))


(defn of-type
  "Gets all entities of the given type"
  [auth resource routes & {:keys [include-trashed] :or {include-trashed false}}]
  (let [db (couch/db auth)]
    (-> (couch/get-view db k/ENTITIES-BY-TYPE-VIEW {:key          resource
                                                  :reduce       false
                                                  :include_docs true})
        (tr/entities-from-couch routes)
        (filter-trashed include-trashed))))



(defn get-entities
  "Gets entities by ID"
  [auth ids routes & {:keys [include-trashed] :or {include-trashed false}}]
  (let [db (couch/db auth)
        docs (filter :_id (couch/all-docs db ids))]
    (-> docs
      (tr/entities-from-couch routes)
      (filter-trashed include-trashed))))

(defn get-values
  "Get values by ID"
  [auth ids & {:keys [routes]}]
  (let [db (couch/db auth)
        docs (couch/all-docs db ids)]
    (if routes
      (tr/values-from-couch docs routes)
      docs)))

;; COMMAND

(defn parent-collaboration-roots
  [auth parent routes]
  (if (nil? parent)
    []
    (if-let [doc (first (get-entities auth [parent] routes))]
      (-> doc
        :links
        :_collaboration_roots)
      [])))


(defn create-entities
  "POSTs entity(s) with the given parent and owner"
  [auth entities routes & {:keys [parent] :or {parent nil}}]
  (let [db (couch/db auth)]

    (when (some #{k/USER-ENTITY} (map :type entities))
      (throw+ {:type ::auth/unauthorized :message "You can't create a User via the Ovation REST API"}))

    (couch/bulk-docs db
      (tw/to-couch (auth/authenticated-user-id auth)
        entities
        :collaboration_roots (parent-collaboration-roots auth parent routes)))))

(defn create-values
  "POSTs value(s) direct to Couch"
  [auth routes values]

  (when-not (every? #{k/ANNOTATION-TYPE k/RELATION-TYPE} (map :type values))
    (throw+ {:type ::illegal-argument :message "Values must have :type \"Annotation\" or \"Relation\""}))

  (let [db (couch/db auth)
        docs (map (auth/check! (auth/authenticated-user-id auth) ::auth/create) values)]
    (tr/values-from-couch (couch/bulk-docs db docs) routes)))

(defn- merge-updates
  [updates]
  (let [updated-attributes (into {} (map (fn [update] [(str (:_id update)) {:attributes (:attributes update)
                                                                            :rev        (:_rev update)
                                                                            :_collaboration_roots (get-in update [:links :_collaboration_roots])}]) updates))]
    (fn [doc]
      (let [update (updated-attributes (str (:_id doc)))
            roots (:_collaboration_roots update)
            updated-roots (if (nil? roots) doc (assoc-in doc [:links :_collaboration_roots] (:_collaboration_roots update)))]
        (assoc updated-roots :attributes (:attributes update)
                             :_rev (:rev update))))))


(defn update-entities
  "Updates entities{EntityUpdate} or creates entities. If :direct true, PUTs entities directly, otherwise,
  updates only entity attributes from lastest rev"
  [auth entities routes & {:keys [direct] :or [direct false]}]
  (let [db (couch/db auth)]

    (when (some #{k/USER-ENTITY} (map :type entities))
      (throw+ {:type ::auth/unauthorized :message "Not authorized to update a User"}))

    (let [bulk-docs (if direct
                      entities
                      (let [ids  (map :_id entities)
                            docs (get-entities auth ids routes)
                            updated-docs (map (merge-updates entities) docs)]
                        (vals (merge (util/into-id-map entities) (util/into-id-map updated-docs)))))
          auth-checked-docs (doall (map (auth/check! (auth/authenticated-user-id auth) :auth/update) bulk-docs))]
      (tr/entities-from-couch (couch/bulk-docs db (tw/to-couch (auth/authenticated-user-id auth) auth-checked-docs)) routes))))

(defn trash-entity
  [user-id doc]
  (let [info {(keyword k/TRASHING-USER) user-id
              (keyword k/TRASHING-DATE) (util/iso-now)
              (keyword k/TRASH-ROOT)    (:_id doc)}]
    (if-not (nil? (:trash_info doc))
      (throw+ {:type ::illegal-operation :message "Entity is already trashed"}))
    (assoc doc :trash_info info)))


(defn delete-entity
  [auth ids routes]
  (let [db (couch/db auth)
        docs (get-entities auth ids routes)]

    (when (some #{k/USER-ENTITY} (map :type docs))
      (throw+ {:type ::auth/unauthorized :message "Not authorized to trash a User"}))

    (let [user-id (auth/authenticated-user-id auth)
          trashed (map #(trash-entity user-id %) docs)
          auth-checked-docs (vec (map (auth/check! user-id :auth/delete) trashed))]
      (couch/bulk-docs db auth-checked-docs))))


(defn delete-values
  "DELETEs value(s) direct to Couch"
  [auth ids routes]

  (let [values (get-values auth ids)]
    (when-not (every? #{k/ANNOTATION-TYPE k/RELATION-TYPE} (map :type values))
      (throw+ {:type ::illegal-argument :message "Values must have :type \"Annotation\" or \"Relation\""}))

    (let [db (couch/db auth)
          docs (map (auth/check! (auth/authenticated-user-id auth) ::auth/delete) values)]
      (tr/values-from-couch (couch/delete-docs db docs) routes))))
