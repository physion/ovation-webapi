(ns ovation.core
  (:require [ovation.couch :as couch]
            [ovation.transform.read :as tr]
            [ovation.transform.write :as tw]
            [ovation.auth :as auth]
            [slingshot.slingshot :refer [throw+ try+]]
            [ovation.util :as util]
            [ovation.constants :as k]
            [com.climate.newrelic.trace :refer [defn-traced]]))



;; QUERY
(defn filter-trashed
  "Removes entity documents with a non-nil trash_info from seq"
  [entities include_trashed]
  (filter #(or include_trashed (nil? (:trash_info %))) entities))


(defn-traced of-type
  "Gets all entities of the given type"
  [auth db resource routes & {:keys [include-trashed] :or {include-trashed false}}]

  (-> (couch/get-view auth db k/ENTITIES-BY-TYPE-VIEW {:key          resource
                                                       :reduce       false
                                                       :include_docs true})
    (tr/entities-from-couch auth routes)
    (filter-trashed include-trashed)))



(defn-traced get-entities
  "Gets entities by ID"
  [auth db ids routes & {:keys [include-trashed] :or {include-trashed false}}]
  (-> (couch/all-docs auth db ids)
    (tr/entities-from-couch auth routes)
    (filter-trashed include-trashed)))

(defn-traced get-entity
  [auth id routes  & {:keys [include-trashed] :or {include-trashed false}}]
  (first (get-entities auth [id] routes :include-trashed include-trashed)))

(defn-traced get-values
  "Get values by ID"
  [auth db ids & {:keys [routes]}]
  (let [docs (couch/all-docs auth db ids)]
    (if routes
      (tr/values-from-couch docs auth routes)
      docs)))

(defn-traced get-owner
  [auth db routes entity]
  (first (get-entities auth db [(:owner entity)] routes)))

;; COMMAND

(defn-traced parent-collaboration-roots
  [auth db parent routes]
  (if (nil? parent)
    []
    (if-let [doc (first (get-entities auth db [parent] routes))]
      (-> doc
        :links
        :_collaboration_roots)
      [])))

(defn-traced create-entities
  "POSTs entity(s) with the given parent and owner"
  [auth db entities routes & {:keys [parent] :or {parent nil}}]

  (when (some #{k/USER-ENTITY} (map :type entities))
    (throw+ {:type ::auth/unauthorized :message "You can't create a User via the Ovation REST API"}))


  (let [created-entities (tr/entities-from-couch (couch/bulk-docs db
                                                   (tw/to-couch (auth/authenticated-user-id auth)
                                                     entities
                                                     :collaboration_roots (parent-collaboration-roots auth db parent routes)))
                           auth
                           routes)]

    created-entities))

(defn- write-values
  [auth db routes values op]
  (when-not (every? #{k/ANNOTATION-TYPE k/RELATION-TYPE} (map :type values))
    (throw+ {:type ::illegal-argument :message "Values must have :type \"Annotation\" or \"Relation\""}))

  (let [docs (map (auth/check! auth op) values)]
    (tr/values-from-couch (couch/bulk-docs db docs) auth routes)))

(defn-traced create-values
  "POSTs value(s) direct to Couch"
  [auth db routes values]

  (write-values auth db routes values ::auth/create))

(defn-traced update-values
  "PUTs value(s) direct to Couch"
  [auth db routes values]

  (write-values auth db routes values ::auth/update))

(defn- merge-updates-fn
  [updates & {:keys [update-collaboration-roots allow-keys] :or [allow-keys []]}]

  (let [updated-attributes (into {} (map (fn [update] [(str (:_id update)) {:attributes           (:attributes update)
                                                                            :rev                  (:_rev update)
                                                                            :_collaboration_roots (get-in update [:links :_collaboration_roots])
                                                                            :allowed-keys         (select-keys update allow-keys)}]) updates))]
    (fn [doc]
      (let [update (updated-attributes (str (:_id doc)))
            roots  (if update-collaboration-roots (:_collaboration_roots update) (get-in doc [:links :_collaboration_roots]))]
        (-> doc
          (assoc-in [:links :_collaboration_roots] (or roots [])) ; Update collaboration roots
          (merge (:allowed-keys update))                    ; Merge additional allowed keys
          (assoc :attributes (:attributes update)           ; Update attributes and rev
                 :_rev (:rev update)))))))


(defn-traced update-entities
  "Updates entities{EntityUpdate} or creates entities. If :allow-keys is non-empty, allows extra keys. Otherwise,
  updates only entity attributes from lastest rev"
  [auth db entities routes & {:keys [authorize update-collaboration-roots allow-keys] :or {authorize                  true
                                                                                           update-collaboration-roots false
                                                                                           allow-keys                 []}}]

  (when (some #{k/USER-ENTITY} (map :type entities))
    (throw+ {:type ::auth/unauthorized :message "Not authorized to update a User"}))

  (let [bulk-docs         (let [ids      (map :_id entities)
                                docs     (get-entities auth db ids routes)
                                merge-fn (merge-updates-fn entities :update-collaboration-roots update-collaboration-roots :allow-keys allow-keys)]
                            (map merge-fn docs))
        auth-checked-docs (if authorize (doall (map (auth/check! auth ::auth/update) bulk-docs)) bulk-docs)]
    (tr/entities-from-couch (couch/bulk-docs db (tw/to-couch (auth/authenticated-user-id auth) auth-checked-docs))
      auth
      routes)))

(defn-traced trash-entity
  [user-id doc]
  (let [info {(keyword k/TRASHING-USER) user-id
              (keyword k/TRASHING-DATE) (util/iso-now)
              (keyword k/TRASH-ROOT)    (:_id doc)}]
    (if-not (nil? (:trash_info doc))
      (throw+ {:type ::illegal-operation :message "Entity is already trashed"}))
    (assoc doc :trash_info info)))

(defn-traced restore-trashed-entity
  [auth doc]
  (dissoc doc :trash_info))

(defn-traced restore-deleted-entities
  [auth db ids routes]
  (let [docs              (get-entities auth ids routes :include-trashed true)
        user-id           (auth/authenticated-user-id auth)
        trashed           (map #(restore-trashed-entity user-id %) docs)
        auth-checked-docs (vec (map (auth/check! auth ::auth/update) trashed))]
    (tr/entities-from-couch (couch/bulk-docs db (tw/to-couch (auth/authenticated-user-id auth) auth-checked-docs))
      auth
      routes)))

(defn-traced delete-entities
  [auth db ids routes]
  (let [docs (get-entities auth db ids routes)]

    (when (some #{k/USER-ENTITY} (map :type docs))
      (throw+ {:type ::auth/unauthorized :message "Not authorized to trash a User"}))

    (let [user-id (auth/authenticated-user-id auth)
          trashed (map #(trash-entity user-id %) docs)
          auth-checked-docs (vec (map (auth/check! auth ::auth/delete) trashed))]
      (tr/entities-from-couch (couch/bulk-docs db (tw/to-couch (auth/authenticated-user-id auth) auth-checked-docs))
                              auth
                              routes))))


(defn-traced delete-values
  "DELETEs value(s) direct to Couch"
  [auth db ids routes]

  (let [values (get-values auth db ids)]
    (when-not (every? #{k/ANNOTATION-TYPE k/RELATION-TYPE} (map :type values))
      (throw+ {:type ::illegal-argument :message "Values must have :type \"Annotation\" or \"Relation\""}))

    (let [docs (map (auth/check! auth ::auth/delete) values)]
      (tr/values-from-couch (couch/delete-docs db docs) auth routes))))
