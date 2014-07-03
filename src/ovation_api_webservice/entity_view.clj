(ns ovation-api-webservice.entity-view
  (:use ovation-api-webservice.util)
  (:require clojure.pprint)
)

(defn get-entity-helper [uuid api_key]
  (entities-to-json
    (seq [(-> (ctx api_key) (.getObjectWithUuid (parse-uuid uuid)))])
  )
)

(defn get-entity [id request]
  (auth-filter request (partial get-entity-helper id))
)

