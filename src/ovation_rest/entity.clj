(ns ovation-rest.entity
  (:use ovation-rest.util))

(defn json-to-map [json]
  (->
    (new com.fasterxml.jackson.databind.ObjectMapper)
    (.registerModule (com.fasterxml.jackson.datatype.guava.GuavaModule.))
    (.registerModule (com.fasterxml.jackson.datatype.joda.JodaModule.))
    (.readValue json java.util.Map)
    )
  )


(defn get-entity-helper
  "Helper to return the json array for a single entity after retrieving from the database"
  [uuid api_key]
  (into-seq-array
    (seq [(-> (ctx api_key) (.getObjectWithUuid (parse-uuid uuid)))])
    )
  )

(defn get-entity
  "Gets a single entity by ID (uuid)"
  [id request]
  (auth-filter-middleware request (partial get-entity-helper id))
  )

(defn get-entity-rel-helper
  "Helper to return the json array of the target of an entity relation by entity [UU]ID and relation name"
  [uuid rel api_key]
  (let [
         entity (-> (ctx api_key) (. getObjectWithUuid (parse-uuid uuid)))
         relation (-> entity (. getEntities rel))
         ]

    (into-seq-array (seq relation))
    )
  )

(defn get-entity-rel
  "Gets the target of an entity relation by entity ID and relation name for the given request"
  [id rel request]
  (auth-filter-middleware request (partial get-entity-rel-helper id rel))
  )

(defn update-entity-helper [uuid request api_key]
  (let [
         body (unmunge-strings (get-body-from-request request) (host-from-request request))
         in_json (json-to-map body)
         ]
    (do
      (-> (ctx api_key) (.getObjectWithUuid (parse-uuid uuid)) (.update in_json))
      (get-entity-helper uuid api_key)
      )
    )
  )

(defn update-entity [id request]
  (auth-filter-middleware request (partial update-entity-helper id request))
  )

(defn create-multimap [m]
  (us.physion.ovation.util.MultimapUtils/createMultimap m)
  )

(defn create-entity-helper [request api_key]
  (let [
         body (get-body-from-request request)
         json_map (into {} (json-to-map body))
         entity (-> (ctx api_key)
                    (.insertEntity
                      (-> json_map
                          (update-in ["links"] create-multimap)
                          )
                      )
                    )
         ]
    (into-seq-array (seq [entity]))
    )
  )

(defn create-entity [request]
  (auth-filter-middleware request (partial create-entity-helper request))
  )

(defn delete-entity-helper [uuid request api_key]
  (let [
         entity (-> (ctx api_key) (. getObjectWithUuid (parse-uuid uuid)))
         trash_resp (-> (ctx api_key) (. trash entity) (.get))
         ]
    (str "{\"success\": 1}")
    )
  )

(defn delete-entity [id request]
  (auth-filter-middleware request (partial delete-entity-helper id request))
  )

(defn index-resource [resource request api_key]
  (let [
        resources (case resource
                    "project" (-> (ctx api_key) (.getProjects))
                    "source" (-> (ctx api_key) (.getTopLevelSources))
                    "protocol" (-> (ctx api_key) (.getProtocols))
                    )]

    (seq (into-seq-array resources request))))

