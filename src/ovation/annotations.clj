(ns ovation.annotations
  (:require [ovation.version :refer [version-path]]))

(defn add-annotation-links                                  ;;keep
  "Add links for annotation types to entity .links"
  [e]
  (let [prefix (clojure.string/join ["/api" version-path "/entities/" (:_id e) "/annotations/"])
        properties {:properties (clojure.string/join [prefix "properties"])}
        tags {:tags (clojure.string/join [prefix "tags"])}
        timeline-events {:timeline-events (clojure.string/join [prefix "timeline-events"])}
        notes {:notes (clojure.string/join [prefix "notes"])}]
    (assoc-in e [:links] (merge properties tags timeline-events notes (:links e)))))

(defn union-annotations-map
  "Collects all annotation documents in a top-level annotations or annotations type map"
  [annotations]
  (flatten (map (fn [v]
                  (if (instance? java.util.Map v)
                    (union-annotations-map (seq v))
                    (seq v))) (vals annotations))))
