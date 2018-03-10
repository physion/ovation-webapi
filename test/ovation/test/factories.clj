(ns ovation.test.factories
  (:require [ovation.db.activities :as activities]
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
            [ovation.test.db.organizations :as organizations]
            [ovation.test.db.teams :as teams]
            [ovation.test.db.users :as users]
            [ovation.util :as util]))

(defn activity
  [db attrs]
  (if-not (contains? attrs :organization_id)
    (throw (AssertionError. "Missing required key :organization_id")))
  (if-not (contains? attrs :project_id)
    (throw (AssertionError. "Missing required key :project_id")))
  (if-not (contains? attrs :owner_id)
    (throw (AssertionError. "Missing required key :owner_id")))
  (let [record (merge {:_id (str (util/make-uuid))
                       :name "An Activity"
                       :created-at (util/iso-now)
                       :updated-at (util/iso-now)
                       :attributes (util/to-json {})} attrs)
        activity-id (:generated_key (activities/create db record))]
    (-> record
      (assoc :id activity-id))))

(defn file
  [db attrs]
  (if-not (contains? attrs :organization_id)
    (throw (AssertionError. "Missing required key :organization_id")))
  (if-not (contains? attrs :project_id)
    (throw (AssertionError. "Missing required key :project_id")))
  (if-not (contains? attrs :owner_id)
    (throw (AssertionError. "Missing required key :owner_id")))
  (let [record (merge {:_id (str (util/make-uuid))
                       :name "A File"
                       :attributes (util/to-json {})
                       :created-at (util/iso-now)
                       :updated-at (util/iso-now)} attrs)
        file-id (:generated_key (files/create db record))]
    (-> record
      (assoc :id file-id))))

(defn folder
  [db attrs]
  (if-not (contains? attrs :organization_id)
    (throw (AssertionError. "Missing required key :organization_id")))
  (if-not (contains? attrs :project_id)
    (throw (AssertionError. "Missing required key :project_id")))
  (if-not (contains? attrs :owner_id)
    (throw (AssertionError. "Missing required key :owner_id")))
  (let [record (merge {:_id (str (util/make-uuid))
                       :name "A Folder"
                       :attributes (util/to-json {})
                       :created-at (util/iso-now)
                       :updated-at (util/iso-now)} attrs)
        folder-id (:generated_key (folders/create db record))]
    (-> record
      (assoc :id folder-id))))

(defn note
  [db attrs]
  (if-not (contains? attrs :user_id)
    (throw (AssertionError. "Missing required key :user_id")))
  (if-not (contains? attrs :organization_id)
    (throw (AssertionError. "Missing required key :organization_id")))
  (if-not (contains? attrs :project_id)
    (throw (AssertionError. "Missing required key :project_id")))
  (if-not (contains? attrs :entity_id)
    (throw (AssertionError. "Missing required key :entity_id")))
  (if-not (contains? attrs :entity_type)
    (throw (AssertionError. "Missing required key :entity_type")))
  (let [record (merge {:_id (str (util/make-uuid))
                       :text "Some note"
                       :timestamp (util/iso-now)
                       :created-at (util/iso-now)
                       :updated-at (util/iso-now)} attrs)
        note-id (:generated_key (notes/create db record))]
    (-> record
      (assoc :id note-id))))

(defn organization
  [db attrs]
  (if-not (contains? attrs :owner_id)
    (throw (AssertionError. "Missing require key :owner_id")))
  (let [record (merge {:_id (str (util/make-uuid))
                       :name "Ovation"} attrs)
        org-id (:generated_key (organizations/create db record))]
    (-> record
      (assoc :id org-id))))

(defn project
  [db attrs]
  (if-not (contains? attrs :organization_id)
    (throw (AssertionError. "Missing required key :organization_id")))
  (if-not (contains? attrs :team_id)
    (throw (AssertionError. "Missing required key :team_id")))
  (if-not (contains? attrs :owner_id)
    (throw (AssertionError. "Missing require key :owner_id")))
  (let [record (merge {:_id (str (util/make-uuid))
                       :name "A Project"
                       :attributes (util/to-json {})
                       :created-at (util/iso-now)
                       :updated-at (util/iso-now)} attrs)
        project-id (:generated_key (projects/create db record))]
    (-> record
      (assoc :id project-id))))

(defn property
  [db attrs]
  (if-not (contains? attrs :user_id)
    (throw (AssertionError. "Missing required key :user_id")))
  (if-not (contains? attrs :organization_id)
    (throw (AssertionError. "Missing required key :organization_id")))
  (if-not (contains? attrs :project_id)
    (throw (AssertionError. "Missing required key :project_id")))
  (if-not (contains? attrs :entity_id)
    (throw (AssertionError. "Missing required key :entity_id")))
  (if-not (contains? attrs :entity_type)
    (throw (AssertionError. "Missing required key :entity_type")))
  (let [record (merge {:_id (str (util/make-uuid))
                       :key "Key"
                       :value "Value"
                       :created-at (util/iso-now)
                       :updated-at (util/iso-now)} attrs)
        property-id (:generated_key (properties/create db record))]
    (-> record
      (assoc :id property-id))))

(defn relation
  [db attrs]
  (if-not (contains? attrs :user_id)
    (throw (AssertionError. "Missing required key :user_id")))
  (if-not (contains? attrs :organization_id)
    (throw (AssertionError. "Missing required key :organization_id")))
  (if-not (contains? attrs :project_id)
    (throw (AssertionError. "Missing required key :project_id")))
  (if-not (contains? attrs :source_id)
    (throw (AssertionError. "Missing required key :source_id")))
  (if-not (contains? attrs :source_type)
    (throw (AssertionError. "Missing required key :source_type")))
  (if-not (contains? attrs :target_id)
    (throw (AssertionError. "Missing required key :target_id")))
  (if-not (contains? attrs :target_type)
    (throw (AssertionError. "Missing required key :target_type")))
  (let [record (merge {:_id (str (util/make-uuid))
                       :rel "Parent"
                       :inverse_rel "Child"} attrs)
        relation-id (:generated_key (relations/create db record))]
    (-> record
      (assoc :id relation-id))))

(defn revision
  [db attrs]
  (if-not (contains? attrs :organization_id)
    (throw (AssertionError. "Missing required key :organization_id")))
  (if-not (contains? attrs :owner_id)
    (throw (AssertionError. "Missing required key :owner_id")))
  (if-not (contains? attrs :file_id)
    (throw (AssertionError. "Missing required key :file_id")))
  (let [record (merge {:_id (str (util/make-uuid))
                       :resource_id nil
                       :name "A File"
                       :version nil
                       :content_type "text/plain"
                       :content_length nil
                       :upload_status nil
                       :url nil
                       :attributes (util/to-json {})
                       :created-at (util/iso-now)
                       :updated-at (util/iso-now)} attrs)
        revision-id (:generated_key (revisions/create db record))]
    (-> record
      (assoc :id revision-id))))

(defn source
  [db attrs]
  (if-not (contains? attrs :organization_id)
    (throw (AssertionError. "Missing required key :organization_id")))
  (if-not (contains? attrs :owner_id)
    (throw (AssertionError. "Missing required key :owner_id")))
  (let [record (merge {:_id (str (util/make-uuid))
                       :resource_id nil
                       :name "A Source"
                       :attributes (util/to-json {})
                       :created-at (util/iso-now)
                       :updated-at (util/iso-now)} attrs)
        source-id (:generated_key (sources/create db record))]
    (-> record
      (assoc :id source-id))))

(defn tag
  [db attrs]
  (if-not (contains? attrs :user_id)
    (throw (AssertionError. "Missing required key :user_id")))
  (if-not (contains? attrs :organization_id)
    (throw (AssertionError. "Missing required key :organization_id")))
  (if-not (contains? attrs :project_id)
    (throw (AssertionError. "Missing required key :project_id")))
  (if-not (contains? attrs :entity_id)
    (throw (AssertionError. "Missing required key :entity_id")))
  (if-not (contains? attrs :entity_type)
    (throw (AssertionError. "Missing required key :entity_type")))
  (let [record (merge {:_id (str (util/make-uuid))
                       :tag "Tag"
                       :created-at (util/iso-now)
                       :updated-at (util/iso-now)} attrs)
        tag-id (:generated_key (tags/create db record))]
    (-> record
      (assoc :id tag-id))))

(defn team
  [db attrs]
  (if-not (contains? attrs :owner_id)
    (throw (AssertionError. "Missing required key :owner_id")))
  (let [uuid (str (util/make-uuid))
        record (merge {:_id uuid
                       :name uuid} attrs)
        team-id (:generated_key (teams/create db record))]
    (-> record
      (assoc :id team-id))))

(defn timeline_event
  [db attrs]
  (if-not (contains? attrs :user_id)
    (throw (AssertionError. "Missing required key :user_id")))
  (if-not (contains? attrs :organization_id)
    (throw (AssertionError. "Missing required key :organization_id")))
  (if-not (contains? attrs :project_id)
    (throw (AssertionError. "Missing required key :project_id")))
  (if-not (contains? attrs :entity_id)
    (throw (AssertionError. "Missing required key :entity_id")))
  (if-not (contains? attrs :entity_type)
    (throw (AssertionError. "Missing required key :entity_type")))
  (let [record (merge {:_id (str (util/make-uuid))
                       :name "Timeline event"
                       :notes "A note"
                       :start (util/iso-now)
                       :end (util/iso-now)
                       :created-at (util/iso-now)
                       :updated-at (util/iso-now)} attrs)
        timeline-event-id (:generated_key (timeline_events/create db record))]
    (-> record
      (assoc :id timeline-event-id))))

(defn user
  [db attrs]
  (let [record (merge {:_id (str (util/make-uuid))
                       :first_name "John"
                       :last_name "Doe"
                       :created_at (util/iso-now)
                       :updated_at (util/iso-now)} attrs)
        user-id (:generated_key (users/create db record))]
    (-> record
      (assoc :id user-id))))


