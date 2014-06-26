(ns ovation-api-webservice.protocol-view
  (:use ovation-api-webservice.util)
)

(defn index-protocol-helper [api_key]
  (entities-to-json 
    (seq (-> (ctx api_key) (. getProtocols)))
  )
)

(defn get-protocol-helper [uuid api_key]
  (entities-to-json 
    (seq [(-> (ctx api_key) (. getObjectWithUuid (java.util.UUID/fromString uuid)))])
  )
)

(defn update-protocol-helper [uuid request api_key]
  (let [
        body (unmunge-strings (get-body-from-request request) (host-from-request request))
        in_json (json-to-object body)
        do_convert (-> (ctx api_key) (. getObjectWithUuid (java.util.UUID/fromString uuid)) (.update in_json))
       ]
    (get-protocol-helper uuid api_key)
  )
)

(defn create-protocol-helper [request api_key]
  (let [
        body (get-body-from-request request)
        in_json (json-to-object body)
        protocol (-> (ctx api_key) (.insertProtocol (.get in_json "name") (.get in_json "procedure")))
       ]
    (entities-to-json (seq [protocol]))
  )
)

(defn delete-protocol-helper [uuid request api_key]
  (let [
        protocol (-> (ctx api_key) (. getObjectWithUuid (java.util.UUID/fromString uuid)))
        trash_resp (-> (ctx api_key) (. trash protocol) (.get))
       ]
    (str "{\"success\": 1}")
  )
)

(defn index-protocol [request]
  (auth-filter request (partial index-protocol-helper))
)

(defn get-protocol [id request]
  (auth-filter request (partial get-protocol-helper id))
)

(defn create-protocol [request]
  (auth-filter request (partial create-protocol-helper request))
)

(defn update-protocol [id request]
  (auth-filter request (partial update-protocol-helper id request))
)

(defn delete-protocol [id request]
  (auth-filter request (partial delete-protocol-helper id request))
)

