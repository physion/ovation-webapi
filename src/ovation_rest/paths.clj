(ns ovation-rest.paths)

(def separator "/")
(def separator-pattern (re-pattern separator))

(defn split
  "Splits a path into components"
  [path]
  (filter #(not (empty? %)) (clojure.string/split (str path) separator-pattern)))

(defn join
  "Joins path components"
  [path-seq & {:keys [trailing-separator] :or {:trailing-separator false}}]
  (let [joined-path (clojure.string/join separator path-seq)]
    (if trailing-separator
      (format "%s/" joined-path)
      joined-path)))
