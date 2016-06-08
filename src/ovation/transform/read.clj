(ns ovation.transform.read
  (:require [ovation.version :refer [version version-path]]
            [ring.util.http-response :refer [conflict! unauthorized! forbidden!]]
            [ovation.util :as util]
            [ovation.schema :refer [EntityRelationships]]
            [ovation.routes :as r]
            [ovation.constants :as c]
            [ovation.constants :as k]
            [ovation.auth :as auth]
            [clojure.string :as s]))


(defn add-annotation-links                                  ;;keep
  "Add links for annotation types to entity .links"
  [e rt]
  (let [properties {:properties (r/annotations-route rt e "properties")}
        tags {:tags (r/annotations-route rt e "tags")}
        timeline-events {:timeline-events (r/annotations-route rt e "timeline_events")}
        notes {:notes (r/annotations-route rt e "notes")}]
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
  [dto rt]
  (let [entity-type (util/entity-type-keyword dto)
        relationships (EntityRelationships entity-type)
        links (into {} (map (fn [[rel _]]
                              [rel {:self (r/relationship-route rt dto rel)
                                          :related (r/targets-route rt dto rel)}])
                            relationships))]

    (assoc-in dto [:relationships] (merge links (get dto :relationships {})))))

(defn add-heads-link
  [dto rt]
  (if (= (util/entity-type-keyword dto) (util/entity-type-name-keyword k/FILE-TYPE))
    (assoc-in dto [:links :heads] (r/heads-route rt dto))
    dto))

(defn add-self-link
  "Adds self link to dto"
  [dto rt]
  (assoc-in dto [:links :self] (r/self-route rt dto)))

(defn add-annotation-self-link
  "Adds self link to Annotation dto"
  [dto rt]
  (let [annotation-key (:annotation_type dto)
        entity-id      (:entity dto)
        annotation-id  (:_id dto)
        route-name     (keyword (str "delete-" (s/lower-case annotation-key)))]
    (assoc-in dto [:links :self] (r/named-route rt route-name {:id entity-id :annotation-id annotation-id}))))

(defn remove-user-attributes
  "Removes :attributes from User entities"
  [dto]
  (if (= (:type dto) "User")
    (let [m (get dto :attributes {})]
      (assoc dto :attributes (select-keys m (for [[k _] m :when (= k :name)] k))))
    dto))

(defn add-entity-permissions
  [doc auth]
  (-> doc
    (assoc-in [:permissions :create] true)
    (assoc-in [:permissions :update] (auth/can? auth ::auth/update doc))
    (assoc-in [:permissions :delete] (auth/can? auth ::auth/delete doc))))

(defn couch-to-entity
  [auth router]
  (fn [doc]
    (case (:error doc)
      "conflict" (conflict!)
      "forbidden" (forbidden!)
      "unauthorized" (unauthorized!)
      (let [collaboration-roots (get-in doc [:links :_collaboration_roots])]
        (-> doc
          (remove-user-attributes)
          (dissoc :named_links)                           ;; For v3
          (dissoc :links)                                 ;; For v3
          (dissoc :relationships)
          (add-self-link router)
          (add-heads-link router)
          (add-annotation-links router)
          (add-relationship-links router)
          (assoc-in [:links :_collaboration_roots] collaboration-roots)
          (add-entity-permissions auth))))))


(defn entities-from-couch
  "Transform couchdb documents."
  [docs auth router]
  (let [teams (auth/authenticated-teams auth)]
    (->> docs
      (map (couch-to-entity auth router))
      (filter #(auth/can? auth ::auth/read % :teams teams)))))

(defn add-value-permissions
  [doc auth]
  (-> doc
    (assoc-in [:permissions :update] (auth/can? auth ::auth/update doc))
    (assoc-in [:permissions :delete] (auth/can? auth ::auth/delete doc))))

(defn couch-to-value
  [auth router]
  (fn [doc]
    (case (:error doc)
      "conflict" (conflict! doc)
      "forbidden" (forbidden!)
      "unauthorized" (unauthorized!)
      (condp = (util/entity-type-name doc)
        c/RELATION-TYPE-NAME (-> doc
                               (add-self-link router))
        ;(add-value-permissions auth)

        c/ANNOTATION-TYPE-NAME (-> doc
                                 (add-annotation-self-link router)
                                 (add-value-permissions auth))

        ;; default
        (-> doc
          (add-value-permissions auth))))))

(defn values-from-couch
  "Transform couchdb value documents (e.g. LinkInfo)"
  [docs auth router]
  (let [teams (auth/authenticated-teams auth)]
    (->> docs
      (map (couch-to-value auth router))
      (filter #(auth/can? auth ::auth/read % :teams teams)))))
