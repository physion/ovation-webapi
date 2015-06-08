(ns ovation.core
  (:require [ovation.couch :as couch]
            [ovation.transform.read :as tr]
            [ovation.transform.write :as tw]
            [ovation.auth :as auth]
            [slingshot.slingshot :refer [throw+ try+]]
            [ovation.util :as util])
  (:import (us.physion.ovation.data EntityDao$Views)
           (us.physion.ovation.domain TrashInfo)))


;; QUERY
(defn filter-trashed
  "Removes entity documents with a non-nil trash_info from seq"
  [entities include_trashed]
  (filter #(or include_trashed (nil? (:trash_info %))) entities))


(defn of-type
  "Gets all entities of the given type"
  [auth resource & {:keys [include-trashed] :or {include-trashed false}}]

  (-> (map :doc (couch/get-view
                  (couch/db auth)
                  EntityDao$Views/ENTITIES_BY_TYPE
                  {:key resource :reduce false :include_docs true}))
    (tr/from-couch)
    (filter-trashed include-trashed)))



(defn get-entities
  "Gets entities by ID"
  [auth ids & {:keys [include-trashed] :or {include-trashed false}}]
  (let [db (couch/db auth)
        docs (couch/all-docs db ids)]
    (-> docs
      (tr/from-couch)
      (filter-trashed include-trashed))))



;; COMMAND

(defn parent-collaboration-roots
  [auth parent]
  (if (nil? parent)
    []
    (if-let [doc (first (get-entities auth [parent]))]
      (-> doc
        :links
        :_collaboration_roots)
      [])))


(defn create-entity
  "POSTs entity(s) with the given parent and owner"
  [auth entities & {:keys [parent] :or {parent nil}}]
  (let [db (couch/db auth)]
    (couch/bulk-docs db
      (tw/to-couch (auth/authorized-user-id auth)
        entities
        :collaboration_roots (parent-collaboration-roots auth parent)))
    ))

(defn- update-attributes
  [updates]
  (let [updated-attributes (into {} (map (fn [update] [(:_id update) {:attributes (:attributes update)
                                                                      :rev        (:_rev update)}]) updates))]
    (fn [doc]
      (let [update (updated-attributes (:_id doc))]
        (assoc doc :attributes (:attributes update)
                   :_rev (:rev update))))))


(defn update-entity
  "Updates entities{EntityUpdate}"
  [auth entities]
  (let [db (couch/db auth)
        ids (map (fn [e] (:_id e)) entities)
        docs (get-entities auth ids)
        updated-docs (map (update-attributes entities) docs)]
    (doall (map (auth/check! (auth/authorized-user-id auth) :auth/update) updated-docs))
    (couch/bulk-docs db updated-docs)))

(defn trash-entity
  [user-id doc]
  (let [info {(keyword TrashInfo/TRASHING_USER) user-id
              (keyword TrashInfo/TRASHING_DATE) (util/iso-now)
              (keyword TrashInfo/TRASH_ROOT)    (:_id doc)}]
    (if-not (nil? (:trash_info doc))
      (throw+ {:type ::illegal-operation :message "Entity is already trashed"}))
    (assoc doc :trash_info info)))

(defn delete-entity
  [auth ids]
  (let [db (couch/db auth)
        docs (get-entities auth ids)
        user-id (auth/authorized-user-id auth)
        trashed (map #(trash-entity user-id %) docs)]
    (doall (map (auth/check! user-id :auth/delete) trashed))
    (couch/bulk-docs db trashed)))
