(ns ovation-rest.paths)

(def separator "/")
(def separator-pattern (re-pattern separator))

(defn split
  "Splits a path into components"
  [path]
  (filter #(not (empty? %)) (clojure.string/split (str path) separator-pattern)))

(defn join
  "Joins path components"
  [path-vec]
  (clojure.string/join separator path-vec))
