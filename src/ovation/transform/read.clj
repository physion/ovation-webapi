(ns ovation.transform.read
  (:require [ovation.version :refer [version version-path]]
            [ring.util.http-response :refer [not-found!]]
            [ovation.util :as util]
            [ovation.schema :refer [EntityRelationships]]
            [ovation.routes :as r]))


(defn add-annotation-links                                  ;;keep
  "Add links for annotation types to entity .links"
  [e rt]
  (let [id (:_id e)
        prefix (r/annotations-route rt e)
        properties {:properties (clojure.string/join [prefix "/properties"])}
        tags {:tags (clojure.string/join [prefix "/tags"])}
        timeline-events {:timeline-events (clojure.string/join [prefix "/timeline-events"])}
        notes {:notes (clojure.string/join [prefix "/notes"])}]
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
  (let [entity-type (util/entity-type-keyword (:type dto))
        relationships (EntityRelationships entity-type)
        links (into {} (map (fn [[rel _]]
                              [rel {:self (r/relationship-route rt dto rel)
                                    :related (r/targets-route rt dto rel)}]
                              ) relationships))]
    (assoc-in dto [:links] (merge links (get dto :links {})))))

(defn add-self-link
  "Adds self link to dto"
  [dto router]
  (assoc-in dto [:links :self] (r/self-route router dto)))

(defn remove-user-attributes
  "Removes :attributes from User entities"
  [dto]
  (if (= (:type dto) "User")
    (let [m (get dto :attributes {})]
      (assoc dto :attributes (select-keys m (for [[k v] m :when (= k :name)] k))))
    dto))

(defn couch-to-doc
  [router]
  (fn [doc]
    (if (:error doc)
      (not-found! doc)
      (if (and (:type doc) (not (= (str (:type doc)) util/RELATION_TYPE)))
        (let [collaboration-roots (get-in doc [:links :_collaboration_roots])]
          (-> doc
            (remove-user-attributes)
            (remove-private-links)
            (add-self-link router)
            (add-annotation-links router)
            (add-relationship-links router)
            (assoc-in [:links :_collaboration_roots] collaboration-roots)))
        doc))))


(defn from-couch
  "Transform couchdb documents."
  [docs router]
  (map (couch-to-doc router) docs))
