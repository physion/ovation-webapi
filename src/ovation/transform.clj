(ns ovation.transform
  (:require [ovation.version :refer [version-path]]
            [ovation.version :as ver]
            [ovation.util :as util]))


(defn add-annotation-links                                  ;;keep
  "Add links for annotation types to entity .links"
  [e]
  (let [prefix (clojure.string/join ["/api" version-path "/entities/" (:_id e) "/annotations/"])
        properties {:properties (clojure.string/join [prefix "properties"])}
        tags {:tags (clojure.string/join [prefix "tags"])}
        timeline-events {:timeline-events (clojure.string/join [prefix "timeline-events"])}
        notes {:notes (clojure.string/join [prefix "notes"])}]
    (assoc-in e [:links] (merge properties tags timeline-events notes (:links e)))))

(defn remove-private-links                                  ;;keep
  "Removes private links (e.g. _collaboration_roots) from the dto.links"
  [dto]
  (if-let [links (:links dto)]
    (let [hidden-links (filter #(re-matches #"_.+" (name %)) (keys links))
          cleaned (apply dissoc links hidden-links)]
      (assoc-in dto [:links] cleaned))
    dto))

(defn add-self-link
  [link dto]
  (assoc-in dto [:links :self] link))

(defn entity-single-link                                    ;;keep
  "Return a single link from an id and relationship name"
  [id rel]
  (if (= (name rel) "self")
    (clojure.string/join ["/api" version-path "/entities/" id])
    (clojure.string/join ["/api" version-path "/entities/" id "/links/" (name rel)])))


(defn links-to-rel-path                                     ;;keep
  "Augment an entity dto with the links.self reference"
  [dto]
  (let [add_self (merge-with conj dto {:links {:self ""}})
        new_links_map (into {} (map (fn [x] [(first x) (entity-single-link (:_id dto) (first x))]) (:links add_self)))]
    (assoc-in add_self [:links] new_links_map)))

(defn couch-to-doc
  [doc]
  (->> doc
    (remove-private-links)
    (links-to-rel-path)
    (add-annotation-links)       ;; NB must come after links-to-rel-path
    ;;TODO add-self-link
    ))

(defn from-couch
  "Transform couchdb documents."
  [docs]
  (map couch-to-doc docs))


(defn ensure-id
  "Makes sure there's an _id for entity"
  [doc]
  (if (nil? (:_id doc))
    (assoc doc :_id (util/make-uuid))
    doc))

(defn ensure-api-version
  "Insert API version"
  [doc]
  (assoc doc :api_version ver/schema-version))

(defn -add-owner
  "Adds owner link document."

  [auth doc]

  ;;TODO write owner link to document
  doc)


(defn add-collaboration-roots
  [doc roots]
  (assoc-in doc [:links :_collaboration_roots] roots))

(defn doc-to-couch
  [doc]
  (-> doc
    ensure-id
    ensure-api-version
    (add-collaboration-roots [])))                       ;;TODO

(defn to-couch
  "Transform documents for CouchDB"
  [docs]
  (map doc-to-couch docs))
