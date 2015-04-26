(ns ovation.core
  (:require [ovation.couch :as couch]
            [ovation.transform :as transform])
  (:import (us.physion.ovation.data EntityDao$Views)))

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
    (transform/from-couch)
    (filter-trashed include-trashed)))



(defn get-entities
  "Gets entities by ID"
  [auth ids & {:keys [include-trashed] :or {include-trashed false}}]
  (let [db (couch/db auth)]
    (-> (couch/all-docs db ids)
      (transform/from-couch)
      (filter-trashed include-trashed))))

(defn collaboration-roots
  [id parent]
  (ifn?))
(defn create-entity
  "POSTs a new entity with the given parent and owner"
  [auth type attributes & {:keys [parent] :or {parent nil}}]
  (let [db (couch/db auth)]
    (couch/bulk-docs db (transform/to-couch [{:type type :attributes attributes}]))
    ;; get parent ->> :links :_collaboration_roots
    ;; {:type type :attributes attributes} -> to-couch
    ;; + owner link
    ))
