(ns ovation-api-webservice.user-view
  (:use ovation-api-webservice.util)
)

(defn index-user-helper [api_key]
  (entities-to-json 
    (seq (-> (ctx api_key) (. getusers)))
  )
)

(defn get-user-helper [uuid api_key]
  (entities-to-json 
    (seq [(-> (ctx api_key) (. getObjectWithUuid (java.util.UUID/fromString uuid)))])
  )
)

(defn index-user [request]
  (auth-filter request (partial index-user-helper))
)

(defn get-user [id request]
  (auth-filter request (partial get-user-helper id))
)

