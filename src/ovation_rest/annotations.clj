(ns ovation-rest.annotations
  (:require [ovation-rest.version :refer [version-path]]))

(defn add-annotation-links
  "Add links for annotation types to entity .links"
  [e]
  (let [prefix          (clojure.string/join ["/api" version-path "/entities/" (:_id e) "/annotations/"])
        properties      {:properties (clojure.string/join [prefix "properties"])}
        tags            {:tags (clojure.string/join [prefix "tags"])}
        timeline-events {:timeline-events (clojure.string/join [prefix "timeline-events"])}
        notes           {:notes (clojure.string/join [prefix "notes"])}]
    (assoc-in e [:links] (merge properties tags timeline-events notes (:links e)))))

(defn replace-uri-keys-with-usernames
  "Replaces user URI keys in the annotations map with user names"
  [api-key annotations]
  )
