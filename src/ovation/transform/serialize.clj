(ns ovation.transform.serialize
  (:require [clojure.tools.logging :as logging]
            [ovation.util :as util]))

(defn -ids
  [doc]
  (dissoc doc :id :project_id))

(defn entity
  [doc]
  (logging/info "serialize/entity " doc)
  (-> doc
    (-ids)))

(defn entities
  [docs]
  (map entity docs))

(defn value
  [value]
  value)

(defn values
  [values]
  (map value values))
