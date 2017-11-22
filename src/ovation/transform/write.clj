(ns ovation.transform.write
  (:require [ovation.db.projects :as projects]
            [ovation.db.uuids :as uuids]
            [ovation.util :as util]
            [ovation.request-context :as request-context]
            [clojure.tools.logging :as logging]
            [com.climate.newrelic.trace :refer [defn-traced]]))

(defn ensure-organization
  "Adds organization reference"
  [doc ctx]
  (if-not (:organization_id doc)
    (assoc doc :organization_id (::request-context/org ctx))
    doc))

(defn ensure-project
  "Adds project reference"
  [doc db project-id]
  (if project-id
    (if-not (:project_id doc)
      (if-let [project (projects/find-by-uuid db project-id)]
        (assoc doc :project_id (:id project))
        doc)
      doc)
    doc))

(defn ensure-owner
  "Adds owner reference"
  [doc owner-id]
  (if owner-id
    (if-not (:owner_id doc)
      (assoc doc :owner_id owner-id)
      doc)
    doc))

(defn ensure-user
  "Adds user reference"
  [doc owner-id]
  (if owner-id
    (if-not (:user_id doc)
      (assoc doc :user_id owner-id)
      doc)
    doc))

(defn ensure-created-at
  "Adds created at timestamp"
  [doc timestamp]
  (if-not (:created-at doc)
    (if-let [created-at (:created-at (:attributes doc))]
      (assoc doc :created-at created-at)
      (assoc doc :created-at timestamp))
    doc))

(defn add-updated-at
  "Updates updated at timestamp"
  [doc timestamp]
  (assoc doc :updated-at timestamp))

(defn doc-to-db
  [ctx db collaboration-roots doc]
  (let [owner-id (request-context/user-id ctx)
        time (util/iso-short-now)
        project (first (or collaboration-roots (get-in doc [:links :_collaboration_roots] [])))]
    (-> doc
      (ensure-organization ctx)
      (ensure-project db project)
      (ensure-owner owner-id)
      (ensure-user owner-id)
      (ensure-created-at time)
      (add-updated-at time))))

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
    (if-let [uuid (uuids/find-by-uuid db entity-id)]
      (-> value
        (assoc :entity_id (:entity_id uuid))
        (assoc :entity_type (:entity_type uuid)))
      value)
    value))

(defn transform-target
  [value db entity-id]
  (if entity-id
    (if-let [uuid (uuids/find-by-uuid db entity-id)]
      (-> value
        (assoc :child_entity_id (:entity_id uuid))
        (assoc :child_entity_type (:entity_type uuid)))
      value)
    value))

(defn transform-source
  [value db entity-id]
  (if entity-id
    (if-let [uuid (uuids/find-by-uuid db entity-id)]
      (-> value
        (assoc :parent_entity_id (:entity_id uuid))
        (assoc :parent_entity_type (:entity_type uuid)))
      value)
    value))

(defn value-to-db
  [ctx db value]
  (let [user-id (request-context/user-id ctx)
        time (util/iso-short-now)
        project (first (get-in value [:links :_collaboration_roots] []))]
    (-> value
      (ensure-project db project)
      (ensure-user user-id)
      (transform-annotation)
      (transform-entity db (:entity value))
      (transform-target db (:target_id value))
      (transform-source db (:source_id value)))))
