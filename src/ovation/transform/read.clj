(ns ovation.transform.read
  (:require [ovation.version :refer [version version-path]]
            [ring.util.http-response :refer [conflict! unauthorized! forbidden!]]
            [ovation.util :as util]
            [ovation.schema :refer [EntityRelationships]]
            [ovation.routes :as r]
            [ovation.constants :as c]
            [clojure.tools.logging :as logging]
            [ovation.constants :as k]
            [ovation.auth :as auth]))


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
                                            :related (r/targets-route rt dto rel)}]
                              ) relationships))]

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

(defn remove-user-attributes
  "Removes :attributes from User entities"
  [dto]
  (if (= (:type dto) "User")
    (let [m (get dto :attributes {})]
      (assoc dto :attributes (select-keys m (for [[k v] m :when (= k :name)] k))))
    dto))

(defn add-entity-permissions
  [doc authenticated-user]
  (-> doc
    (assoc-in [:permissions :update] (auth/can? authenticated-user :auth/update doc))
    (assoc-in [:permissions :delete] (auth/can? authenticated-user :auth/delete doc))))

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
          (add-entity-permissions (auth/authenticated-user-id auth)))))))


(defn entities-from-couch
  "Transform couchdb documents."
  [docs auth router]
  (map (couch-to-entity auth router) docs))

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
        doc))))

(defn values-from-couch
  "Transform couchdb value documents (e.g. LinkInfo)"
  [docs auth router]
  (map (couch-to-value auth router) docs))
