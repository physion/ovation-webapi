(ns ovation.transform.read
  (:require [ovation.version :refer [version version-path]]
            [ring.util.http-response :refer [conflict! unauthorized! forbidden!]]
            [ovation.util :as util]
            [ovation.schema :refer [EntityRelationships]]
            [ovation.routes :as r]
            [ovation.constants :as c]
            [ovation.constants :as k]
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
  (if (= (util/entity-type-keyword dto) (util/entity-type-name-keyword k/FILE-TYPE))
    (assoc-in dto [:links :heads] (r/heads-route ctx dto))
    dto))

(defn add-zip-link
  [dto ctx]
  (condp = (:type dto)
    k/ACTIVITY-TYPE (assoc-in dto [:links :zip] (r/zip-activity-route ctx dto))
    k/FOLDER-TYPE (assoc-in dto [:links :zip] (r/zip-folder-route ctx dto))
    dto))

(defn add-upload-links
  [dto ctx]
  (if (= (util/entity-type-keyword dto) (util/entity-type-name-keyword k/REVISION-TYPE))
    (-> dto
      (assoc-in [:links :upload-complete] (r/upload-complete-route ctx dto))
      (assoc-in [:links :upload-failed] (r/upload-failed-route ctx dto)))
    dto))

(defn add-team-link
  [dto ctx]
  (condp = (:type dto)
    k/PROJECT-TYPE (assoc-in dto [:links :team] (r/team-route ctx (:_id dto)))
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

(defn remove-user-attributes
  "Removes :attributes from User entities"
  [dto]
  (if (= (:type dto) "User")
    (let [m (get dto :attributes {})]
      (assoc dto :attributes (select-keys m (for [[k _] m :when (= k :name)] k))))
    dto))

(defn add-entity-permissions
  [doc ctx teams]
  (-> doc
    (assoc-in [:permissions :create] true)
    (assoc-in [:permissions :update] (auth/can? ctx ::auth/update doc))
    (assoc-in [:permissions :delete] (auth/can? ctx ::auth/delete doc))))

(defn convert-file-revision-status
  [doc]
  (if (= (:type doc) k/FILE-TYPE)
    (let [revisions (into {} (map (fn [[k,v]] [(name k) v]) (:revisions doc)))]
      (assoc doc :revisions revisions))
    doc))

(defn couch-to-entity
  [ctx & {:keys [teams] :or {teams nil}}]
  (let [{auth ::request-context/identity} ctx]
    (fn [doc]
      (case (:error doc)
        "conflict" (conflict!)
        "forbidden" (forbidden!)
        "unauthorized" (unauthorized!)
        (let [collaboration-roots (get-in doc [:links :_collaboration_roots])
              org-id (:organization doc)]
          (-> doc
            (remove-user-attributes)
            (dissoc :named_links)                           ;; For v3
            (dissoc :links)                                 ;; For v3
            (dissoc :relationships)
            (dissoc :organization)
            (assoc :organization_id org-id)
            (add-self-link ctx)
            (add-heads-link ctx)
            (add-upload-links ctx)
            (add-zip-link ctx)
            (add-team-link ctx)
            (add-annotation-links ctx)
            (add-relationship-links ctx)
            (convert-file-revision-status)
            (assoc-in [:links :_collaboration_roots] collaboration-roots)
            (add-entity-permissions ctx teams)))))))


(defn-traced entities-from-couch
  "Transform couchdb documents."
  [docs ctx]
  (let [{auth ::request-context/identity} ctx
        teams (auth/authenticated-teams auth)
        xf    (comp
                (map (couch-to-entity ctx))
                (filter #(auth/can? ctx ::auth/read % :teams teams)))]
    (sequence xf docs)))

(defn add-value-permissions
  [doc ctx]
  (-> doc
    (assoc-in [:permissions :update] (auth/can? ctx ::auth/update doc))
    (assoc-in [:permissions :delete] (auth/can? ctx ::auth/delete doc))))

(defn-traced couch-to-value
  [ctx]
  (fn [doc]
    (let [org-id (:organization doc)]
      (case (:error doc)
        "conflict" (conflict! doc)
        "forbidden" (forbidden!)
        "unauthorized" (unauthorized!)
        (condp = (util/entity-type-name doc)
          c/RELATION-TYPE-NAME (-> doc
                                 (dissoc :organization)
                                 (assoc :organization_id org-id)
                                 (add-self-link ctx))
          ;(add-value-permissions auth)

          c/ANNOTATION-TYPE-NAME (-> doc
                                   (dissoc :organization)
                                   (assoc :organization_id org-id)
                                   (add-annotation-self-link ctx)
                                   (add-value-permissions ctx))

          ;; default
          (-> doc
            (dissoc :organization)
            (assoc :organization_id org-id)
            (add-value-permissions ctx)))))))

(defn values-from-couch
  "Transform couchdb value documents (e.g. LinkInfo)"
  [docs ctx]
  (let [{auth ::request-context/identity} ctx
        teams (auth/authenticated-teams auth)]
    (->> docs
      (map (couch-to-value ctx))
      (filter #(auth/can? ctx ::auth/read % :teams teams)))))
