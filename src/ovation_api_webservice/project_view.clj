(ns ovation-api-webservice.project-view
  (:use ovation-api-webservice.util)
)

(defn projects-to-json [api_key]
  (entities-to-json 
    (seq (-> (ctx api_key) (. getProjects)))
  )
)

(defn project-to-json [uuid api_key]
  (entities-to-json 
    (seq [(-> (ctx api_key) (. getObjectWithUuid (java.util.UUID/fromString uuid)))])
  )
)

(defn index-project [request]
  (auth-filter request (partial projects-to-json))
)

(defn get-project [id request]
  (auth-filter request (partial project-to-json id))
)

(defn create-project [request]
  "TODO"
)

(defn update-project [request]
  "TODO"
)

(defn delete-project [request]
  "TODO"
)


