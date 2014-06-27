(ns ovation-api-webservice.project-view
  (:use ovation-api-webservice.util)
  (:require clojure.pprint)
)

(defn index-project-helper [api_key]
  (entities-to-json
    (seq (-> (ctx api_key) (. getProjects)))
  )
)

(defn get-project-helper [uuid api_key]
  (entities-to-json
    (seq [(-> (ctx api_key) (. getObjectWithUuid (parse-uuid uuid)))])
  )
)

(defn update-project-helper [uuid request api_key]
  (let [
        body (unmunge-strings (get-body-from-request request) (host-from-request request))
        in_json (json-to-object body)
       ]
    (do
      (-> (ctx api_key) (. getObjectWithUuid (parse-uuid uuid)) (.update in_json))
      (get-project-helper uuid api_key))
  )
)

(defn create-project-helper [request api_key]
  (let [
        body (get-body-from-request request)
        in_json (json-to-object body)
        project (-> (ctx api_key) (.insertProject (.get in_json "name") (.get in_json "purpose") (new org.joda.time.DateTime (.get in_json "start"))))
       ]
    (entities-to-json (seq [project]))
  )
)

(defn delete-project-helper [uuid request api_key]
  (let [
        project (-> (ctx api_key) (. getObjectWithUuid (parse-uuid uuid)))
        trash_resp (-> (ctx api_key) (. trash project) (.get))
       ]
    (str "{\"success\": 1}")
  )
)

(defn index-project [request]
  (auth-filter request (partial index-project-helper))
)

(defn get-project [id request]
  (auth-filter request (partial get-project-helper id))
)

(defn create-project [request]
  (auth-filter request (partial create-project-helper request))
)

(defn update-project [id request]
  (auth-filter request (partial update-project-helper id request))
)

(defn delete-project [id request]
  (auth-filter request (partial delete-project-helper id request))
)

