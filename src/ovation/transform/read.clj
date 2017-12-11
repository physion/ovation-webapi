(ns ovation.transform.read
  (:require [clojure.tools.logging :as logging]
            [ovation.version :refer [version version-path]]
            [ring.util.http-response :refer [conflict! unauthorized! forbidden!]]
            [ovation.util :as util]
            [ovation.schema :refer [EntityRelationships]]
            [ovation.routes :as r]
            [ovation.constants :as c]
            [ovation.auth :as auth]
            [ovation.request-context :as request-context]
            [clojure.string :as s]
            [com.climate.newrelic.trace :refer [defn-traced]]))


(defn add-annotation-links                                  ;;keep
  "Add links for annotation types to entity .links"
  [e ctx]
  (let [properties {:properties (r/annotations-route ctx e "properties")}
        tags {:tags (r/annotations-route ctx e "tags")}
        timeline-events {:timeline-events (r/annotations-route ctx e "timeline_events")}
        notes {:notes (r/annotations-route ctx e "notes")}]
    (assoc-in e [:links] (merge properties tags timeline-events notes (:links e)))))

(defn remove-private-links
  "Removes private links (e.g. _collaboration_roots) from the dto.links"
  [dto]
  (if-let [links (:links dto)]
    (let [hidden-links (filter #(re-matches #"_.+" (name %)) (keys links))
          cleaned (apply dissoc links hidden-links)]
      (assoc-in dto [:links] cleaned))
    dto))


(defn add-relationship-links
  "Adds relationship links for Couch document and router"
  [dto ctx]
  (let [entity-type (util/entity-type-keyword dto)
        relationships (EntityRelationships entity-type)
        links (into {} (map (fn [[rel _]]
                              [rel {:self (r/relationship-route ctx dto rel)
                                          :related (r/targets-route ctx dto rel)}])
                            relationships))]

    (assoc-in dto [:relationships] (merge links (get dto :relationships {})))))

(defn add-heads-link
  [dto ctx]
  (if (= (util/entity-type-keyword dto) (util/entity-type-name-keyword c/FILE-TYPE))
    (assoc-in dto [:links :heads] (r/heads-route ctx dto))
    dto))

(defn add-zip-link
  [dto ctx]
  (condp = (:type dto)
    c/ACTIVITY-TYPE (assoc-in dto [:links :zip] (r/zip-activity-route ctx dto))
    c/FOLDER-TYPE (assoc-in dto [:links :zip] (r/zip-folder-route ctx dto))
    dto))

(defn add-upload-links
  [dto ctx]
  (if (= (util/entity-type-keyword dto) (util/entity-type-name-keyword c/REVISION-TYPE))
    (-> dto
      (assoc-in [:links :upload-complete] (r/upload-complete-route ctx dto))
      (assoc-in [:links :upload-failed] (r/upload-failed-route ctx dto)))
    dto))

(defn add-team-link
  [dto ctx]
  (condp = (:type dto)
    c/PROJECT-TYPE (assoc-in dto [:links :team] (r/team-route ctx (:_id dto)))
    dto))

(defn add-self-link
  "Adds self link to dto"
  [dto ctx]
  (assoc-in dto [:links :self] (r/self-route ctx dto)))

(defn add-annotation-self-link
  "Adds self link to Annotation dto"
  [dto ctx]
  (let [annotation-key (:annotation_type dto)
        entity-id      (:entity dto)
        annotation-id  (:_id dto)
        route-name     (keyword (str "delete-" (s/lower-case annotation-key)))
        org (::request-context/org ctx)]
    (assoc-in dto [:links :self] (r/named-route ctx route-name {:org org :id entity-id :annotation-id annotation-id}))))

(defn add-collaboration-roots
  [doc ctx]
  (if-let [project-id (:project doc)]
    (-> doc
      (assoc-in [:links :_collaboration_roots] [project-id])
      (dissoc :project))
    doc))

(defn remove-user-attributes
  "Removes :attributes from User entities"
  [dto]
  (if (= (:type dto) "User")
    (let [m (get dto :attributes {})]
      (assoc dto :attributes (select-keys m (for [[k _] m :when (= k :name)] k))))
    dto))

(defn add-entity-permissions
  [doc ctx]
  (-> doc
    (assoc-in [:permissions :create] true)
    (assoc-in [:permissions :update] (auth/can? ctx ::auth/update doc))
    (assoc-in [:permissions :delete] (auth/can? ctx ::auth/delete doc))))

(defn convert-file-revision-status
  [doc]
  (if (= (:type doc) c/FILE-TYPE)
    (let [revisions (into {} (map (fn [[k,v]] [(name k) v]) (:revisions doc)))]
      (assoc doc :revisions revisions))
    doc))

(defn deserialize-attributes
  [doc]
  (if-let [attributes (:attributes doc)]
    (assoc doc :attributes (or (and (string? attributes) (util/from-json attributes)) attributes))
    doc))

(defn -to-timestamp
  [timestamp-or-string]
  (if (string? timestamp-or-string)
    timestamp-or-string
    (util/timestamp-to-iso timestamp-or-string)))

(defn -clean-attributes
  [doc]
  (dissoc doc :name
              :file_id
              :version
              :content_type
              :content_length
              :upload-status
              :url
              :created-at
              :updated-at))

(defn -extract-attributes
  [doc]
  (select-keys doc [:name
                    :file_id
                    :version
                    :content_type
                    :content_length
                    :upload-status
                    :url]))

(defn -expand-attributes
  [doc]
  (let [attributes (or (:attributes doc) {})
        created-at (-to-timestamp (:created-at doc))
        updated-at (-to-timestamp (:updated-at doc))]
    (-> attributes
      (merge (-extract-attributes doc))
      (assoc :created-at created-at)
      (assoc :updated-at updated-at))))

(defn transform-attributes
  [doc]
  (-> doc
    (assoc :attributes (-expand-attributes doc))
    (-clean-attributes)))

(defn remove-id
  [doc]
  (dissoc doc :id))

(defn db-to-entity
  [ctx]
  (fn [record]
    (-> record
      (add-self-link ctx)
      (add-heads-link ctx)
      (add-upload-links ctx)
      (add-zip-link ctx)
      (add-team-link ctx)
      (add-annotation-links ctx)
      (add-relationship-links ctx)
      (add-collaboration-roots ctx)
      (convert-file-revision-status)
      (add-entity-permissions ctx)
      (deserialize-attributes)
      (transform-attributes))))

(defn entities-from-db
  "Transform db records"
  [records ctx]
  (logging/info "entities-from-db " records)
  (let [{auth ::request-context/identity} ctx
        teams (auth/authenticated-teams auth)
        xf    (comp
                (map (db-to-entity ctx))
                (filter #(auth/can? ctx ::auth/read % :teams teams)))]
    (sequence xf records)))

(defn entity-from-db
  "Transform db record"
  [record ctx]
  (first (entities-from-db [record] ctx)))

(defn add-value-permissions
  [doc ctx]
  (-> doc
    (assoc-in [:permissions :update] (auth/can? ctx ::auth/update doc))
    (assoc-in [:permissions :delete] (auth/can? ctx ::auth/delete doc))))

(defn add-annotation
  [doc ctx]
  (condp = (:annotation_type doc)
    c/NOTES (-> doc
              (assoc-in [:annotation] {:text (:text doc)
                                       :timestamp (-to-timestamp (:timestamp doc))
                                       :edited_at (-to-timestamp (:edited_at doc))})
              (dissoc :text)
              (dissoc :timestamp)
              (dissoc :edited_at))
    c/PROPERTIES (-> doc
                   (assoc-in [:annotation] {:key (:key doc)
                                            :value (:value doc)})
                   (dissoc :key)
                   (dissoc :value))
    c/TAGS (-> doc
             (assoc-in [:annotation] {:tag (:tag doc)})
             (dissoc :tag))
    c/TIMELINE_EVENTS (-> doc
                        (assoc-in [:annotation] {:name (:name doc)
                                                 :notes (:notes doc)
                                                 :start (-to-timestamp (:start doc))
                                                 :end (-to-timestamp (:end doc))})
                        (dissoc :name)
                        (dissoc :notes)
                        (dissoc :start)
                        (dissoc :end))))

(defn add-relation-collaboration-roots
  [doc ctx]
  (-> doc
    (assoc-in [:links :_collaboration_roots] [(:source_id doc)])))

(defn-traced db-to-value
  [ctx]
  (fn [doc]
    (condp = (:type doc)
      c/RELATION-TYPE (-> doc
                        (add-relation-collaboration-roots ctx)
                        (add-self-link ctx)
                        (remove-id))
      c/ANNOTATION-TYPE (-> doc
                          (add-annotation ctx)
                          (add-collaboration-roots ctx)
                          (add-value-permissions ctx)
                          (remove-id))
      ;; default
      (-> doc
        (add-value-permissions ctx)
        (remove-id)))))


(defn values-from-db
  "Transform db value documents"
  [records ctx]
  (logging/info "values-from-db " records)
  (let [{auth ::request-context/identity} ctx
        teams (auth/authenticated-teams auth)]
    (->> records
      (map (db-to-value ctx))
      (filter #(auth/can? ctx ::auth/read % :teams teams)))))
