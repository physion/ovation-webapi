(ns ovation.transform.write
  (:require [clojure.tools.logging :as logging]
            [ovation.auth :as auth]
            [ovation.constants :as c]
            [ovation.db.files :as files]
            [ovation.db.projects :as projects]
            [ovation.db.uuids :as uuids]
            [ovation.util :as util]
            [ovation.request-context :as request-context]
            [clojure.tools.logging :as logging]
            [com.climate.newrelic.trace :refer [defn-traced]]))

(defn ensure-organization
  "Adds organization reference"
  [doc ctx]
  (logging/info "ensure-organization" doc)
  (if-not (:organization_id doc)
    (assoc doc :organization_id (::request-context/org ctx))
    doc))

(defn ensure-project
  "Adds project reference"
  [doc ctx db project-id]
  (logging/info "ensure-project" doc project-id)
  (if project-id
    (if-not (:project_id doc)
      (let [{auth ::request-context/identity,
             org-id ::request-context/org} ctx
            teams (auth/authenticated-teams auth)]
        (logging/info "ensure-project check" (:parent_entity_type doc) (= (:parent_entity_type doc) c/SOURCE-TYPE))
        (if (= (:parent_entity_type doc) c/SOURCE-TYPE)
          (assoc doc :project_id 0)
          (if-let [project (projects/find-by-uuid db {:id project-id
                                                      :team_uuids teams
                                                      :organization_id org-id})]
            (assoc doc :project_id (:id project))
            doc)))
      doc)
    doc))

(defn transform-file-id
  [doc ctx db file-id]
  (if file-id
    (let [{auth ::request-context/identity,
           org-id ::request-context/org} ctx
          teams (auth/authenticated-teams auth)]
      (if-let [file (files/find-by-uuid db {:id file-id
                                            :team_uuids teams
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

(defn doc-to-db
  [ctx db collaboration-roots doc]
  (logging/info "doc-to-db" doc)
  (let [{auth ::request-context/identity} ctx
        owner-id (auth/authenticated-user-id auth)
        time (util/iso-now)
        project (first (or collaboration-roots (get-in doc [:links :_collaboration_roots] [])))
        attributes (or (:attributes doc) {})
        file (:file_id attributes)
        resource (:resource_id attributes)]
    (-> doc
      (ensure-organization ctx)
      (ensure-project ctx db project)
      (ensure-owner owner-id)
      (ensure-user owner-id)
      (transform-attributes)
      (transform-file-id ctx db file)
      (ensure-created-at time)
      (add-updated-at time)
      (serialize-attributes))))

(defn-traced to-db
  "Transform documents for DB"
  [ctx db docs & {:keys [collaboration_roots] :or {collaboration_roots nil}}]
  (logging/info "Transforming to db" docs)
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
        (assoc :child_entity_id (:entity_id uuid))
        (assoc :child_entity_type (:entity_type uuid)))
      value)
    value))

(defn transform-source
  [value db entity-id]
  (if entity-id
    (if-let [uuid (uuids/find-by-uuid db {:uuid entity-id})]
      (-> value
        (assoc :parent_entity_id (:entity_id uuid))
        (assoc :parent_entity_type (:entity_type uuid)))
      value)
    value))

(defn value-to-db
  [ctx db value]
  (let [{auth ::request-context/identity} ctx
        user-id (auth/authenticated-user-id auth)
        time (util/iso-now)
        project (first (get-in value [:links :_collaboration_roots] []))]
    (logging/info "user-id " user-id)
    (-> value
      (transform-entity db (:entity value))
      (transform-target db (:target_id value))
      (transform-source db (:source_id value))
      (ensure-project ctx db project)
      (ensure-user user-id)
      (add-created-at time)
      (add-updated-at time)
      (transform-annotation))))
