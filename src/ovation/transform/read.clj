(ns ovation.transform.read
  (:require [ovation.version :refer [version version-path]]
            [ring.util.http-response :refer [not-found!]]
            [ovation.util :as util]))


(defn add-annotation-links                                  ;;keep
  "Add links for annotation types to entity .links"
  [e]
  (let [prefix (util/join-path ["/api" version "entities" (:_id e) "annotations"])
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

(defn link-rel-path                                    ;;keep
  "Return a single link from an id and relationship name"
  [id rel]
  (condp = (name rel)
        "self" (util/join-path ["/api" version "entities" id])
        (util/join-path ["/api" version "entities" id "links" (name rel)])))

(defn named-link-rel-path
  [rel id name]
  (str (util/join-path ["/api" version "entities" id "links" (clojure.core/name rel)]) "?name=" (clojure.core/name name)))

(defn make-rel-links
  [id links link-path-fn]
  (into {} (map (fn [x] (let [rel (first x)]
                          [rel (link-path-fn id rel)])) links)))

(defn links-to-rel-path
  "Make :links and :named_links into web API relative paths"
  [dto]
  (let [links (make-rel-links (:_id dto) (:links dto) link-rel-path)
        named-links (into {} (map (fn [x]
                                    (let [rel (first x)
                                          m (second x)]
                                      [rel (make-rel-links (:_id dto) m (partial named-link-rel-path rel))]))) (:named_links dto))
        ](str (:_id update))
    (-> dto
      (assoc-in [:links] links)
      (assoc-in [:named_links] named-links))))

(defn add-self-link
  "Adds self link to dto"
  [dto]
  (assoc-in dto [:links :self] (link-rel-path (:_id dto) :self)))

(defn couch-to-doc
  [doc]
  (if (:error doc)
    (not-found! doc)
    (let [collaboration-roots (get-in doc [:links :_collaboration_roots])]
      (-> doc
        (remove-private-links)
        (links-to-rel-path)
        (add-annotation-links)                              ;; NB must come after links-to-rel-path
        (add-self-link)
        (assoc-in [:links :_collaboration_roots] collaboration-roots)))))


(defn from-couch
  "Transform couchdb documents."
  [docs]
  (map couch-to-doc docs))
