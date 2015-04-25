(ns ovation.annotations
  (:require [ovation.version :refer [version-path]]))


(defn union-annotations-map
  "Collects all annotation documents in a top-level annotations or annotations type map"
  [annotations]
  (flatten (map (fn [v]
                  (if (instance? java.util.Map v)
                    (union-annotations-map (seq v))
                    (seq v))) (vals annotations))))
