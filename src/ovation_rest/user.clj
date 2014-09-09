(ns ovation-rest.user
  (:use ovation-rest.util)
)

(defn index-user-helper [api_key]
  (into-map-array
    (seq (-> (ctx api_key) (. getusers)))
  )
)

(defn get-user-helper [uuid api_key]
  (into-map-array
    (seq [(-> (ctx api_key) (. getObjectWithUuid (parse-uuid uuid)))])
  )
)

(defn index-user [request]
  (auth-filter-middleware request (partial index-user-helper))
)

(defn get-user [id request]
  (auth-filter-middleware request (partial get-user-helper id))
)

