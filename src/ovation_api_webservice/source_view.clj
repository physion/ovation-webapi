(ns ovation-api-webservice.source-view
  (:use ovation-api-webservice.util)
)

(defn index-source-helper [api_key]
  (entities-to-json
    (seq (-> (ctx api_key) (. getSources)))
  )
)

(defn get-source-helper [uuid api_key]
  (entities-to-json
    (seq [(-> (ctx api_key) (. getObjectWithUuid (parse-uuid uuid)))])
  )
)

(defn update-source-helper [uuid request api_key]
  (let [
        body (unmunge-strings (get-body-from-request request) (host-from-request request))
        in_json (json-to-object body)
        do_convert (-> (ctx api_key) (. getObjectWithUuid (parse-uuid uuid)) (.update in_json))
       ]
    (get-source-helper uuid api_key)
  )
)

(defn create-source-helper [request api_key]
  (let [
        body (get-body-from-request request)
        in_json (json-to-object body)
        source (-> (ctx api_key) (.insertSource (.get in_json "label") (.get in_json "identifier")))
       ]
    (entities-to-json (seq [source]))
  )
)

(defn delete-source-helper [uuid request api_key]
  (let [
        source (-> (ctx api_key) (. getObjectWithUuid (parse-uuid uuid)))
        trash_resp (-> (ctx api_key) (. trash source) (.get))
       ]
    (str "{\"success\": 1}")
  )
)

(defn index-source [request]
  (auth-filter request (partial index-source-helper))
)

(defn get-source [id request]
  (auth-filter request (partial get-source-helper id))
)

(defn create-source [request]
  (auth-filter request (partial create-source-helper request))
)

(defn update-source [id request]
  (auth-filter request (partial update-source-helper id request))
)

(defn delete-source [id request]
  (auth-filter request (partial delete-source-helper id request))
)

