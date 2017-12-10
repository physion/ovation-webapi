(ns ovation.transform.serialize
  (:require [clojure.tools.logging :as logging]
            [ovation.util :as util]))

(defn -remove-id
  [doc]
  (dissoc doc :id))

(defn entity
  [doc]
  (logging/info "serialize/entity " doc)
  (-> doc
    (-remove-id)))

(defn entities
  [docs]
  (map entity docs))

(defn value
  [value]
  value)

(defn values
  [values]
  (map value values))
