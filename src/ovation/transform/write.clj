(ns ovation.transform.write
  (:require [ovation.auth :as auth]
            [ovation.constants :as c]
            [ovation.db.files :as files]
            [ovation.db.projects :as projects]
            [ovation.db.uuids :as uuids]
            [ovation.util :as util]
            [ovation.request-context :as request-context]
            [com.climate.newrelic.trace :refer [defn-traced]]))

(defn ensure-organization
  "Adds organization reference"
  [doc ctx]
  (if-not (:organization_id doc)
    (assoc doc :organization_id (::request-context/org ctx))
    doc))

(defn ensure-project
  "Adds project reference"
  [doc ctx db project-id]
  (if project-id
    (if-not (:project_id doc)
      (let [{auth ::request-context/identity,
             org-id ::request-context/org} ctx
            teams (auth/authenticated-teams auth)]
        (if-let [project (projects/find-by-uuid db {:id project-id
                                                    :team_uuids (if (empty? teams) [nil] teams)
                                                    :service_account (auth/service-account auth)
                                                    :organization_id org-id})]
          (assoc doc :project_id (:id project))
          doc))
      doc)
    doc))

(defn transform-file-id
  [doc ctx db file-id]
  (if file-id
    (let [{auth ::request-context/identity,
           org-id ::request-context/org} ctx
          teams (auth/authenticated-teams auth)]
      (if-let [file (files/find-by-uuid db {:id file-id
                                            :team_uuids (if (empty? teams) [nil] teams)
                                            :service_account (auth/service-account auth)
                                            :organization_id org-id})]
        (assoc doc :file_id (:id file))
        doc))
    doc))

(defn ensure-owner
  "Adds owner reference"
  [doc owner-id]
  (if owner-id
    (assoc doc :owner_id owner-id)
    doc))

(defn ensure-user
  "Adds user reference"
  [doc owner-id]
  (if owner-id
    (assoc doc :user_id owner-id)
    doc))

(defn ensure-resource-id
  "Add resource reference"
  [doc resource-id]
  (if resource-id
    (assoc doc :resource_id resource-id)
    doc))

(defn ensure-version
  "Add version"
  [doc version]
  (if version
    (assoc doc :version version)
    (assoc doc :version nil)))

(defn ensure-content-length
  "Add content_length"
  [doc content-length]
  (if content-length
    (assoc doc :content_length content-length)
    (assoc doc :content_length nil)))

(defn -clean-attributes
  [attributes]
  (dissoc attributes :name
                     :file_id
                     :resource_id
                     :version
                     :content_type
                     :content_length
                     :upload-status
                     :url))

(defn transform-attributes
  [doc]
  (let [attributes (:attributes doc)]
    (-> doc
      (merge attributes)
      (assoc :attributes (-clean-attributes attributes)))))

(defn ensure-created-at
  "Adds created at timestamp"
  [doc timestamp]
  (if-not (:created-at doc)
    (if-let [attributes (:attributes doc)]
      (if-let [created-at (:created-at attributes)]
        (-> doc
          (assoc :created-at created-at)
          (assoc :attributes (dissoc attributes :created-at)))
        (assoc doc :created-at timestamp))
      (assoc doc :created-at timestamp))
    doc))

(defn add-created-at
  [doc timestamp]
  (if-not (:created-at doc)
    (assoc doc :created-at timestamp)
    doc))

(defn add-updated-at
  "Updates updated at timestamp"
  [doc timestamp]
  (if-let [attributes (:attributes doc)]
    (-> doc
      (assoc :attributes (dissoc attributes :updated-at))
      (assoc :updated-at timestamp))
    (assoc doc :updated-at timestamp)))

(defn serialize-attributes
  [doc]
  (if-let [attributes (:attributes doc)]
    (assoc doc :attributes (util/to-json attributes))
    doc))

(defn -maybe-convert-timestamp
  [timestamp]
  (if (nil? timestamp)
    nil
    (if (string? timestamp)
      (if (> (count timestamp) 23)
        (util/joda-timestamp-to-iso (util/parse-iso-short timestamp))
        timestamp)
      (util/timestamp-to-iso timestamp))))

(defn transform-timestamps
  [record]
  (let [created-at (:created-at record)
        updated-at (:updated-at record)
        timestamp  (:timestamp record)
        edited-at  (:edited_at record)
        start      (:start record)
        end        (:end record)]
    (-> record
      (assoc :created-at (-maybe-convert-timestamp created-at))
      (assoc :updated-at (-maybe-convert-timestamp updated-at))
      (assoc :timestamp  (-maybe-convert-timestamp timestamp))
      (assoc :edited_at  (-maybe-convert-timestamp edited-at))
      (assoc :start      (-maybe-convert-timestamp start))
      (assoc :end        (-maybe-convert-timestamp end)))))

(defn doc-to-db
  [ctx db collaboration-roots doc]
  (let [{auth ::request-context/identity} ctx
        owner-id (auth/authenticated-user-id auth)
        time (util/iso-now)
        project (first (or collaboration-roots (get-in doc [:links :_collaboration_roots] [])))
        attributes (or (:attributes doc) {})
        file (:file_id attributes)
        resource-id (:resource_id attributes)
        version (:version attributes)
        content-length (:content_length attributes)]
    (-> doc
      (ensure-organization ctx)
      (ensure-project ctx db project)
      (ensure-owner owner-id)
      (ensure-user owner-id)
      (ensure-resource-id resource-id)
      (ensure-version version)
      (ensure-content-length content-length)
      (transform-attributes)
      (transform-file-id ctx db file)
      (ensure-created-at time)
      (add-updated-at time)
      (transform-timestamps)
      (serialize-attributes))))

(defn-traced to-db
  "Transform documents for DB"
  [ctx db docs & {:keys [collaboration_roots] :or {collaboration_roots nil}}]
  (map #(doc-to-db ctx db collaboration_roots %) docs))

(defn transform-annotation
  [value]
  (if-let [annotation (:annotation value)]
    (merge value annotation)
    value))

(defn transform-entity
  [value db entity-id]
  (if entity-id
    (if-let [uuid (uuids/find-by-uuid db {:uuid entity-id})]
      (-> value
        (assoc :entity_id (:entity_id uuid))
        (assoc :entity_type (:entity_type uuid)))
      value)
    value))

(defn transform-target
  [value db entity-id]
  (if entity-id
    (if-let [uuid (uuids/find-by-uuid db {:uuid entity-id})]
      (-> value
        (assoc :target_id (:entity_id uuid))
        (assoc :target_type (:entity_type uuid)))
      value)
    value))

(defn transform-source
  [value db entity-id]
  (if entity-id
    (if-let [uuid (uuids/find-by-uuid db {:uuid entity-id})]
      (-> value
        (assoc :source_id (:entity_id uuid))
        (assoc :source_type (:entity_type uuid)))
      value)
    value))

(defn value-to-db
  [ctx db value]
  (let [{auth ::request-context/identity} ctx
        user-id (auth/authenticated-user-id auth)
        time (util/iso-now)
        project (first (get-in value [:links :_collaboration_roots] []))]
    (-> value
      (transform-entity db (:entity value))
      (transform-target db (:target_id value))
      (transform-source db (:source_id value))
      (ensure-project ctx db project)
      (ensure-user user-id)
      (add-created-at time)
      (add-updated-at time)
      (transform-annotation)
      (transform-timestamps))))
