(ns ovation.core
  (:require [ovation.couch :as couch]
            [ovation.transform.read :as tr]
            [ovation.transform.write :as tw]
            [ovation.auth :as auth]
            [slingshot.slingshot :refer [throw+ try+]]
            [ovation.util :as util]
            [ovation.constants :as k]
            [ovation.request-context :as rc]
            [com.climate.newrelic.trace :refer [defn-traced]]))



;; QUERY
(defn filter-trashed
  "Removes entity documents with a non-nil trash_info from seq"
  [entities include_trashed]
  (filter #(or include_trashed (nil? (:trash_info %))) entities))


(defn-traced of-type
  "Gets all entities of the given type"
  [ctx db resource & {:keys [include-trashed] :or {include-trashed false}}]

  (-> (couch/get-view ctx db k/ENTITIES-BY-TYPE-VIEW {:key          resource
                                                      :reduce       false
                                                      :include_docs true})
    (tr/entities-from-couch ctx)
    (filter-trashed include-trashed)))



(defn-traced get-entities
  "Gets entities by ID"
  [ctx db ids & {:keys [include-trashed] :or {include-trashed false}}]
  (-> (couch/all-docs ctx db ids)
    (tr/entities-from-couch ctx)
    (filter-trashed include-trashed)))

(defn-traced get-entity
  [ctx db id & {:keys [include-trashed] :or {include-trashed false}}]
  (first (get-entities ctx db [id] :include-trashed include-trashed)))

(defn-traced get-values
  "Get values by ID"
  [ctx db ids & {:keys [routes]}]
  (let [docs (couch/all-docs ctx db ids)]
    (if routes
      (tr/values-from-couch docs ctx)
      docs)))

(defn-traced get-owner
  [ctx db entity]
  (first (get-entities ctx db [(:owner entity)])))

;; COMMAND

(defn-traced parent-collaboration-roots
  [ctx db parent]
  (if (nil? parent)
    []
    (if-let [doc (first (get-entities ctx db [parent]))]
      (-> doc
        :links
        :_collaboration_roots)
      [])))

(defn-traced create-entities
  "POSTs entity(s) with the given parent and owner"
  [ctx db entities & {:keys [parent] :or {parent nil}}]

  (when (some #{k/USER-ENTITY} (map :type entities))
    (throw+ {:type ::auth/unauthorized :message "You can't create a User via the Ovation REST API"}))


  (let [{auth ::rc/auth} ctx
        created-entities (tr/entities-from-couch (couch/bulk-docs db
                                                   (tw/to-couch ctx
                                                     entities
                                                     :collaboration_roots (parent-collaboration-roots ctx db parent)))
                           ctx)]

    created-entities))

(defn- write-values
  [ctx db values op]
  (when-not (every? #{k/ANNOTATION-TYPE k/RELATION-TYPE} (map :type values))
    (throw+ {:type ::illegal-argument :message "Values must have :type \"Annotation\" or \"Relation\""}))

  (let [{auth ::rc/auth} ctx
        docs (map (auth/check! auth op) values)]
    (tr/values-from-couch (couch/bulk-docs db docs) ctx)))

(defn-traced create-values
  "POSTs value(s) direct to Couch"
  [ctx db values]

  (write-values ctx db values ::auth/create))

(defn-traced update-values
  "PUTs value(s) direct to Couch"
  [ctx db values]

  (write-values ctx db values ::auth/update))

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
  [ctx db entities & {:keys [authorize update-collaboration-roots allow-keys] :or {authorize                          true
                                                                                           update-collaboration-roots false
                                                                                           allow-keys                 []}}]

  (when (some #{k/USER-ENTITY} (map :type entities))
    (throw+ {:type ::auth/unauthorized :message "Not authorized to update a User"}))

  (let [{auth ::rc/auth} ctx
        bulk-docs         (let [ids      (map :_id entities)
                                docs     (get-entities ctx db ids)
                                merge-fn (merge-updates-fn entities :update-collaboration-roots update-collaboration-roots :allow-keys allow-keys)]
                            (map merge-fn docs))
        auth-checked-docs (if authorize (doall (map (auth/check! auth ::auth/update) bulk-docs)) bulk-docs)]
    (tr/entities-from-couch (couch/bulk-docs db (tw/to-couch ctx auth-checked-docs))
      ctx)))

(defn-traced trash-entity
  [user-id doc]
  (let [info {(keyword k/TRASHING-USER) user-id
              (keyword k/TRASHING-DATE) (util/iso-now)
              (keyword k/TRASH-ROOT)    (:_id doc)}]
    (if-not (nil? (:trash_info doc))
      (throw+ {:type ::illegal-operation :message "Entity is already trashed"}))
    (assoc doc :trash_info info)))

(defn-traced restore-trashed-entity
  [ctx doc]
  (dissoc doc :trash_info))

(defn-traced restore-deleted-entities
  [ctx db ids]
  (let [{auth ::rc/auth} ctx
        docs              (get-entities ctx db ids :include-trashed true)
        user-id           (auth/authenticated-user-id auth)
        trashed           (map #(restore-trashed-entity user-id %) docs)
        auth-checked-docs (vec (map (auth/check! auth ::auth/update) trashed))]
    (tr/entities-from-couch (couch/bulk-docs db (tw/to-couch ctx auth-checked-docs))
      ctx)))

(defn-traced delete-entities
  [ctx db ids]
  (let [{auth ::rc/auth} ctx
        docs (get-entities ctx db ids)]

    (when (some #{k/USER-ENTITY} (map :type docs))
      (throw+ {:type ::auth/unauthorized :message "Not authorized to trash a User"}))

    (let [user-id (auth/authenticated-user-id auth)
          trashed (map #(trash-entity user-id %) docs)
          auth-checked-docs (vec (map (auth/check! auth ::auth/delete) trashed))]
      (tr/entities-from-couch (couch/bulk-docs db (tw/to-couch ctx auth-checked-docs))
                              ctx))))


(defn-traced delete-values
  "DELETEs value(s) direct to Couch"
  [ctx db ids]

  (let [{auth ::rc/auth} ctx
        values (get-values ctx db ids)]
    (when-not (every? #{k/ANNOTATION-TYPE k/RELATION-TYPE} (map :type values))
      (throw+ {:type ::illegal-argument :message "Values must have :type \"Annotation\" or \"Relation\""}))

    (let [docs (map (auth/check! auth ::auth/delete) values)]
      (tr/values-from-couch (couch/delete-docs db docs) ctx))))
