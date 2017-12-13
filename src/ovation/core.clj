(ns ovation.core
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.core.async :as async :refer [chan >!! go go-loop >! <!! <! close!]]
            [clojure.tools.logging :as logging]
            [com.climate.newrelic.trace :refer [defn-traced]]
            [ovation.auth :as auth]
            [ovation.config :as config]
            [ovation.constants :as c]
            [ovation.db.activities :as activities]
            [ovation.db.files :as files]
            [ovation.db.folders :as folders]
            [ovation.db.notes :as notes]
            [ovation.db.projects :as projects]
            [ovation.db.properties :as properties]
            [ovation.db.relations :as relations]
            [ovation.db.revisions :as revisions]
            [ovation.db.sources :as sources]
            [ovation.db.tags :as tags]
            [ovation.db.timeline_events :as timeline_events]
            [ovation.db.uuids :as uuids]
            [ovation.pubsub :as pubsub]
            [ovation.request-context :as rc]
            [ovation.teams :as teams]
            [ovation.transform.read :as tr]
            [ovation.transform.write :as tw]
            [ovation.util :as util]
            [slingshot.slingshot :refer [throw+ try+]]))

(defn -publish-updates
  [publisher docs & {:keys [channel close?] :or {channel (chan)
                                                 close?  true}}]
  (let [msg-fn   (fn [doc]
                   {:id           (str (:_id doc))
                    :type         (:type doc)
                    :organization (:organization_id doc)})
        topic    (config/config :db-updates-topic :default :updates)
        channels (map #(pubsub/publish publisher topic (msg-fn %) (chan)) docs)]

    (async/pipe (async/merge channels) channel close?)))

(defn publish-updates
  [db docs]
  (let [{pubsub     :pubsub} db
        publisher (:publisher pubsub)
        pub-ch nil] ;; TODO Fix me
    (-publish-updates publisher docs :channel pub-ch)))

;; QUERY
(defn get-annotation
  "Get Annotiation by ID"
  [ctx db id & {:keys [include-trashed] :or {include-trashed false}}]
  (let [{org-id ::rc/org} ctx
        args {:id id,
              :archived include-trashed
              :organization_id org-id}]
    (-> (or (notes/find-by-uuid db args)
            (properties/find-by-uuid db args)
            (tags/find-by-uuid db args)
            (timeline_events/find-by-uuid db args))
      (tr/entity-from-db ctx))))

(defn -get-entities
  "Get all entities"
  [ctx db-fn & {:keys [include-trashed] :or {include-trashed false}}]
  (let [{org-id ::rc/org, auth ::rc/identity} ctx
        teams (auth/authenticated-teams auth)
        user (auth/authenticated-user-id auth)]
    (logging/info "-get-entities " {:archived include-trashed
                                    :organization_id org-id
                                    :team_uuids teams
                                    :owner_id user})
    (-> (db-fn {:archived include-trashed
                :organization_id org-id
                :team_uuids teams
                :owner_id user})
      (tr/entities-from-db ctx))))

(defn -get-entities-by-id
  "Get all entities by ID"
  [ctx db-fn ids & {:keys [include-trashed] :or {include-trashed false}}]
  (let [{org-id ::rc/org, auth ::rc/identity} ctx
        teams (auth/authenticated-teams auth)
        user (auth/authenticated-user-id auth)
        ids-str (map str ids)]
    (logging/debug "-get-entities-by-id " {:ids ids-str
                                           :organization_id org-id
                                           :team_uuids teams
                                           :owner_id user})
    (-> (db-fn {:ids ids-str
                :archived include-trashed
                :organization_id org-id
                :team_uuids teams
                :owner_id user})
      (tr/entities-from-db ctx))))

(defn of-type
  "Gets all entities of the given type"
  [ctx db resource & {:keys [include-trashed] :or {include-trashed false}}]

  (condp = resource
    c/ACTIVITY-TYPE (-get-entities ctx (partial activities/find-all db) :include-trashed include-trashed)
    c/FILE-TYPE     (-get-entities ctx (partial files/find-all db)      :include-trashed include-trashed)
    c/FOLDER-TYPE   (-get-entities ctx (partial folders/find-all db)    :include-trashed include-trashed)
    c/PROJECT-TYPE  (-get-entities ctx (partial projects/find-all db)   :include-trashed include-trashed)
    c/REVISION-TYPE (-get-entities ctx (partial revisions/find-all db)  :include-trashed include-trashed)
    c/SOURCE-TYPE   (-get-entities ctx (partial sources/find-all db)    :include-trashed include-trashed)
    :else []))

(defn -get-activities-by-id
  "Get all activities by ID"
  [ctx db ids & {:keys [include-trashed] :or {include-trashed false}}]
  (-get-entities-by-id ctx (partial activities/find-all-by-uuid db) ids :include-trashed include-trashed))

(defn -get-files-by-id
  "Get all files by ID"
  [ctx db ids & {:keys [include-trashed] :or {include-trashed false}}]
  (-get-entities-by-id ctx (partial files/find-all-by-uuid db) ids :include-trashed include-trashed))

(defn -get-folders-by-id
  "Get all folders by ID"
  [ctx db ids & {:keys [include-trashed] :or {include-trashed false}}]
  (-get-entities-by-id ctx (partial folders/find-all-by-uuid db) ids :include-trashed include-trashed))

(defn -get-projects-by-id
  "Get all projects by ID"
  [ctx db ids & {:keys [include-trashed] :or {include-trashed false}}]
  (-get-entities-by-id ctx (partial projects/find-all-by-uuid db) ids :include-trashed include-trashed))

(defn -get-revisions-by-id
  "Get all revisions by ID"
  [ctx db ids & {:keys [include-trashed] :or {include-trashed false}}]
  (-get-entities-by-id ctx (partial revisions/find-all-by-uuid db) ids :include-trashed include-trashed))

(defn -get-sources-by-id
  "Get all sources by ID"
  [ctx db ids & {:keys [include-trashed] :or {include-trashed false}}]
  (-get-entities-by-id ctx (partial sources/find-all-by-uuid db) ids :include-trashed include-trashed))

(defn -get-projects
  "Get projects"
  [ctx db & {:keys [include-trashed] :or {include-trashed false}}]
  (-get-entities ctx (partial projects/find-all db) :include-trashed include-trashed))

(defn get-entities
  "Gets entities by ID"
  [ctx db ids & {:keys [include-trashed] :or {include-trashed false}}]
  (if (empty? ids)
    []
    (concat (-get-activities-by-id ctx db ids :include-trashed include-trashed)
            (-get-files-by-id      ctx db ids :include-trashed include-trashed)
            (-get-folders-by-id    ctx db ids :include-trashed include-trashed)
            (-get-projects-by-id   ctx db ids :include-trashed include-trashed)
            (-get-revisions-by-id  ctx db ids :include-trashed include-trashed)
            (-get-sources-by-id    ctx db ids :include-trashed include-trashed))))

(defn-traced get-entity
  [ctx db id & {:keys [include-trashed] :or {include-trashed false}}]
  (first (get-entities ctx db [id] :include-trashed include-trashed)))

(defn -get-values-by-id
  [ctx db-fn ids]
  (let [{org-id ::rc/org, auth ::rc/identity} ctx
        teams (auth/authenticated-teams auth)]
    (-> (db-fn {:ids ids
                :organization_id org-id
                :team_uuids teams})
      (tr/values-from-db ctx))))

(defn get-notes-by-id
  "Get all notes by ID"
  [ctx db ids]
  (-get-values-by-id ctx (partial notes/find-all-by-uuid db) ids))

(defn get-properties-by-id
  "Get all properties by ID"
  [ctx db ids]
  (-get-values-by-id ctx (partial properties/find-all-by-uuid db) ids))

(defn get-relations-by-id
  "Get all relations by ID"
  [ctx db ids]
  (-get-values-by-id ctx (partial relations/find-all-by-uuid db) ids))

(defn get-tags-by-id
  "Get all tags by ID"
  [ctx db ids]
  (-get-values-by-id ctx (partial tags/find-all-by-uuid db) ids))

(defn get-timeline-events-by-id
  "Get all timeline events by ID"
  [ctx db ids]
  (-get-values-by-id ctx (partial timeline_events/find-all-by-uuid db) ids))

(defn get-values
  "Get values by ID"
  [ctx db ids]
  (concat (get-notes-by-id           ctx db ids)
          (get-properties-by-id      ctx db ids)
          (get-relations-by-id       ctx db ids)
          (get-tags-by-id            ctx db ids)
          (get-timeline-events-by-id ctx db ids)))

(defn-traced get-owner
  [ctx db entity]
  (first (get-entities ctx db [(:owner entity)])))

;; COMMAND
(defn-traced parent-collaboration-roots
  [ctx db parent-id]
  (if (nil? parent-id)
    []
    (if-let [doc (first (get-entities ctx db [parent-id]))]
      (condp = (:type doc)
        c/PROJECT-TYPE [(:_id doc)]
        ;; default
        (get-in doc [:links :_collaboration_roots]))
      [])))

(defn --create-entity
  [ctx db entity & {:keys [collaboration_roots] :or {collaboration_roots nil}}]
  (let [{org-id ::rc/org, auth ::rc/identity} ctx
        user-id (auth/authenticated-user-id auth)
        user-uuid (auth/authenticated-user-uuid auth)
        team (or (and (= (:type entity) c/PROJECT-TYPE) (teams/create-team ctx db (:_id entity))) {})
        updated-entity (assoc entity :team_id (:id team))
        record (first (tw/to-db ctx db [updated-entity] :collaboration_roots collaboration_roots
                                                        :organization_id org-id
                                                        :user_id user-id))
        _ (logging/info "--create-entity record" record)
        result (condp = (:type entity)
                 c/ACTIVITY-TYPE (activities/create db record)
                 c/FILE-TYPE     (files/create db record)
                 c/FOLDER-TYPE   (folders/create db record)
                 c/PROJECT-TYPE  (projects/create db record)
                 c/REVISION-TYPE (revisions/create db record)
                 c/SOURCE-TYPE   (sources/create db record))]
      (-> record
        (assoc :id (:generated_key result))
        (assoc :owner user-uuid)
        (dissoc :user_id)
        (dissoc :owner_id)
        (dissoc :team_id))))

(defn -create-entity
  "Create entity with given parent and owner"
  [ctx db doc & {:keys [collaboration_roots] :or {collaboration_roots nil}}]
  (let [uuid (str (or (:_id doc) (util/make-uuid)))
        collaboration-roots (or collaboration_roots [uuid])
        updated-doc (assoc doc :_id uuid)
        entity-type (:type updated-doc)
        _ (logging/info "UUID " uuid " Doc " updated-doc)
        uuid-entry {:uuid uuid
                    :entity_type entity-type
                    :created-at (util/iso-now)
                    :updated-at (util/iso-now)}
        uuid-result (uuids/create db uuid-entry)
        record (--create-entity ctx db updated-doc :collaboration_roots collaboration-roots)]
    (uuids/update-entity db (-> uuid-entry
                              (assoc :entity_id (:id record))
                              (assoc :updated-at (util/iso-now))))
    ((tr/db-to-entity ctx) record)))

(defn -create-entities-tx
  [ctx db entities & {:keys [parent] :or {parent nil}}]
  (let [collaboration-roots (parent-collaboration-roots ctx db parent)]
    (jdbc/with-db-transaction [tx db]
      (doall (map #(-create-entity ctx db % :collaboration_roots collaboration-roots) entities)))))

(defn create-entities
  "Creates entity(s) with the given parent and owner"
  [ctx db entities & {:keys [parent] :or {parent nil}}]

  (when (some #{c/USER-ENTITY} (map :type entities))
    (throw+ {:type ::auth/unauthorized :message "You can't create a User via the Ovation REST API"}))

  (-create-entities-tx ctx db entities :parent parent))

(defn add-organization
  [ctx]
  (let [org (::rc/org ctx)]
    (fn [doc]
      (assoc doc :organization_id org))))

(defn- -ensure-annotation-or-relation-type
  [values]
  (when-not (every? #{c/ANNOTATION-TYPE c/RELATION-TYPE} (map :type values))
    (throw+ {:type ::illegal-argument :message "Values must have :type \"Annotation\" or \"Relation\""})))

(defn -ensure-organization-and-authorization
  [ctx value op]
  (-> value
    ((add-organization ctx))
    ((auth/check! ctx op))))

;; TODO any create or update operation must publish changes
(defn- -create-relation-value
  [ctx db value]
  (let [record (tw/value-to-db ctx db value)
        _ (logging/info "-create-relation-value " record)
        result (relations/create db record)]
    (-> value
      (assoc :id (:generated_key result)))))

(defn- -create-annotation-value
  [ctx db value]
  (let [record (tw/value-to-db ctx db value)
        result (condp = (:annotation_type value)
                 c/NOTES (notes/create db record)
                 c/PROPERTIES (properties/create db record)
                 c/TAGS (tags/create db record)
                 c/TIMELINE_EVENTS (timeline_events/create db record))]
    (-> value
      (assoc :id (:generated_key result)))))

(defn- -create-value
  [ctx db value]
  (let [authorized-value (-ensure-organization-and-authorization ctx value ::auth/create)
        uuid (str (or (:_id value) (util/make-uuid)))
        updated-value (assoc authorized-value :_id uuid)
        entity-type (:type updated-value)
        uuid-entry {:uuid uuid
                    :entity_type entity-type
                    :created-at (util/iso-now)
                    :updated-at (util/iso-now)}
        is-annotation (= entity-type c/ANNOTATION-TYPE)
        _ (logging/info "uuid-entry " uuid-entry)
        uuid-result (and is-annotation (uuids/create db uuid-entry))
        record (condp = (:type authorized-value)
                 c/RELATION-TYPE (-create-relation-value ctx db updated-value)
                 c/ANNOTATION-TYPE (-create-annotation-value ctx db updated-value))]

    (if is-annotation
      (uuids/update-entity db (-> uuid-entry
                                (assoc :entity_id (:id record))
                                (assoc :updated-at (util/iso-now)))))
    ((tr/db-to-value ctx) record)))

(defn -create-values-tx
  [ctx db values]
  (jdbc/with-db-transaction [tx db]
    (doall (map #(-create-value ctx tx %) values))))

(defn-traced create-values
  "POSTs value(s) direct to Couch"
  [ctx db values]
  (-create-values-tx ctx db values)
  values)

(defn -update-relation-value
  [ctx db value]
  (let [record (tw/value-to-db ctx db value)]
    (relations/update db record)))

(defn -update-annotation-value
  [ctx db value]
  (let [record (tw/value-to-db ctx db value)]
    (condp = (:annotation_type record)
      c/NOTES (notes/update db record)
      c/PROPERTIES (properties/update db record)
      c/TAGS (tags/update db record)
      c/TIMELINE_EVENTS (timeline_events/update db record))))

(defn -update-value
  [ctx db value]
  (let [authorized-value (-ensure-organization-and-authorization ctx value ::auth/update)]
    (condp = (:type authorized-value)
      c/RELATION-TYPE (-update-relation-value ctx db authorized-value)
      c/ANNOTATION-TYPE (-update-annotation-value ctx db authorized-value))))

(defn -update-values-tx
  [ctx db values]
  (jdbc/with-db-transaction [tx db]
    (doall (map #(-update-value ctx tx %) values))))

(defn-traced update-values
  "PUTs value(s) direct to Couch"
  [ctx db values]
  (-update-values-tx ctx db values)
  values)

(defn- merge-updates-fn
  [updates & {:keys [update-collaboration-roots allow-keys] :or [allow-keys []]}]

  (let [updated-attributes (into {} (map (fn [update] [(str (:_id update)) {:attributes           (:attributes update)
                                                                            :_collaboration_roots (get-in update [:links :_collaboration_roots])
                                                                            :allowed-keys         (select-keys update allow-keys)}]) updates))]
    (fn [doc]
      (let [update (updated-attributes (str (:_id doc)))
            roots  (if update-collaboration-roots (:_collaboration_roots update) (get-in doc [:links :_collaboration_roots]))]
        (-> doc
          (assoc-in [:links :_collaboration_roots] (or roots [])) ; Update collaboration roots
          (merge (:allowed-keys update))                    ; Merge additional allowed keys
          (assoc :attributes (:attributes update)))))))     ; Update attributes and rev

(defn -update-entity
  [db record]
  (logging/info "-update-entity " record)
  (condp = (:type record)
    c/ACTIVITY-TYPE (activities/update db record)
    c/FILE-TYPE (files/update db record)
    c/FOLDER-TYPE (folders/update db record)
    c/PROJECT-TYPE (projects/update db record)
    c/REVISION-TYPE (revisions/update db record)
    c/SOURCE-TYPE (sources/update db record)))

(defn -update-entities-tx
  [ctx db docs]
  (jdbc/with-db-transaction [tx db]
    (doall (map #(-update-entity tx %) (tw/to-db ctx db docs)))))

(defn -archive-entity
  [db record]
  (logging/info "-delete-entity " record)
  (condp = (:type record)
    c/ACTIVITY-TYPE (activities/archive db record)
    c/FILE-TYPE (files/archive db record)
    c/FOLDER-TYPE (folders/archive db record)
    c/PROJECT-TYPE (projects/archive db record)
    c/REVISION-TYPE (revisions/archive db record)
    c/SOURCE-TYPE (sources/archive db record)))

(defn -archive-entities-tx
  [ctx db docs]
  (jdbc/with-db-transaction [tx db]
    (doall (map #(-archive-entity tx %) (tw/to-db ctx db docs)))))

(defn -unarchive-entity
  [db record]
  (logging/info "-delete-entity " record)
  (condp = (:type record)
    c/ACTIVITY-TYPE (activities/unarchive db record)
    c/FILE-TYPE (files/unarchive db record)
    c/FOLDER-TYPE (folders/unarchive db record)
    c/PROJECT-TYPE (projects/unarchive db record)
    c/REVISION-TYPE (revisions/unarchive db record)
    c/SOURCE-TYPE (sources/unarchive db record)))

(defn -unarchive-entities-tx
  [ctx db docs]
  (jdbc/with-db-transaction [tx db]
    (doall (map #(-unarchive-entity tx %) (tw/to-db ctx db docs)))))

(defn-traced update-entities
  "Updates entities{EntityUpdate} or creates entities. If :allow-keys is non-empty, allows extra keys. Otherwise,
  updates only entity attributes from lastest rev"
  [ctx db entities & {:keys [authorize update-collaboration-roots allow-keys] :or {authorize                  true
                                                                                   update-collaboration-roots false
                                                                                   allow-keys                 []}}]

  (logging/info "update-entities " entities)
  (when (some #{c/USER-ENTITY} (map :type entities))
    (throw+ {:type ::auth/unauthorized :message "Not authorized to update a User"}))

  (let [ids (map :_id entities)]
    (let [bulk-docs         (let [docs     (get-entities ctx db ids)
                                  merge-fn (merge-updates-fn entities :update-collaboration-roots update-collaboration-roots :allow-keys allow-keys)]
                              (map merge-fn docs))
          auth-checked-docs (if authorize (doall (map (auth/check! ctx ::auth/update) bulk-docs)) bulk-docs)]
      (-update-entities-tx ctx db auth-checked-docs))
    (get-entities ctx db ids)))

(defn restore-deleted-entities
  [ctx db ids]
  (let [{auth ::rc/identity} ctx
        docs              (get-entities ctx db ids :include-trashed true)
        auth-checked-docs (vec (map (auth/check! ctx ::auth/update) docs))]
    (-unarchive-entities-tx ctx db auth-checked-docs)
    docs))

(defn archive-entity
  [user-id record]
  (-> record
    (assoc :archived true)
    (assoc :archived_at (util/iso-now))
    (assoc :archived_by_user_id user-id)))

(defn delete-entities
  [ctx db ids]
  (let [{auth ::rc/identity} ctx
        docs (get-entities ctx db ids)]

    (when (some #{c/USER-ENTITY} (map :type docs))
      (throw+ {:type ::auth/unauthorized :message "Not authorized to trash a User"}))

    (let [user-id (auth/authenticated-user-id auth)
          trashed (map #(archive-entity user-id %) docs)
          auth-checked-docs (vec (map (auth/check! ctx ::auth/delete) trashed))]
      (-archive-entities-tx ctx db auth-checked-docs)
      (get-entities ctx db ids))))

(defn -delete-relation-value
  [db value]
  (relations/delete db value)
  value)

(defn -delete-annotation-value
  [db value]
  (condp = (:annotation_type value)
    c/NOTES (notes/delete db value)
    c/PROPERTIES (properties/delete db value)
    c/TAGS (tags/delete db value)
    c/TIMELINE_EVENTS (timeline_events/delete db value))
  value)

(defn -delete-value
  [db value]
  (condp = (:type value)
    c/RELATION-TYPE (-delete-relation-value db value)
    c/ANNOTATION-TYPE (-delete-annotation-value db value)))

(defn -delete-values-tx
  [db values]
  (jdbc/with-db-transaction [tx db]
    (doall (map #(-delete-value tx %) values))))

(defn delete-values
  "DELETEs value(s)"
  [ctx db ids]

  (let [{auth ::rc/identity} ctx
        values (get-values ctx db ids)]
    (when-not (every? #{c/ANNOTATION-TYPE c/RELATION-TYPE} (map :type values))
      (throw+ {:type ::illegal-argument :message "Values must have :type \"Annotation\" or \"Relation\""}))

    (let [docs (map (auth/check! ctx ::auth/delete) values)]
      (-delete-values-tx db docs))
    values))
