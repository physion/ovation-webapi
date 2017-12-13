(ns ovation.transform.serialize
  (:require [clojure.tools.logging :as logging]
            [ovation.util :as util]))

(defn -remove-ids
  [doc]
  (dissoc doc :id :project :project_id))

(defn -remove-timestamps
  [doc]
  (dissoc doc :created-at :updated-at :timestamp :edited_at :start :end))

(defn entity
  [doc]
  (logging/info "serialize/entity " doc)
  (-> doc
    (-remove-ids)
    (-remove-timestamps)))

(defn entities
  [docs]
  (map entity docs))

(defn value
  [value]
  (-> value
    (-remove-ids)
    (-remove-timestamps)))

(defn values
  [values]
  (map value values))
