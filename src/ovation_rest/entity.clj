(ns ovation-rest.entity
  (:import (us.physion.ovation.domain URIs))
  (:use ovation-rest.util))

(defn- api-key
  "Extracts the API key from request query parameters"
  [request]
  ("api-key" (:query-params request)))

(defn json-to-map [json]
  (->
    (new com.fasterxml.jackson.databind.ObjectMapper)
    (.registerModule (com.fasterxml.jackson.datatype.guava.GuavaModule.))
    (.registerModule (com.fasterxml.jackson.datatype.joda.JodaModule.))
    (.readValue json java.util.Map)
    )
  )


(defn get-entity
  "Gets a single entity by ID (uuid)"
  [api-key uuid host-url]
  (into-map-array
    (seq [(-> (ctx api-key) (.getObjectWithUuid (parse-uuid uuid)))])
    host-url))

;(defn get-entity-rel
;  "Helper to return the json array of the target of an entity relation by entity [UU]ID and relation name"
;  [uuid rel api_key]
;  (let [
;         entity (-> (ctx api_key) (. getObjectWithUuid (parse-uuid uuid)))
;         relation (-> entity (. getEntities rel))
;         ]
;
;    (into-map-array (seq relation))
;    )
;  )


(defn create-multimap [m]
  (us.physion.ovation.util.MultimapUtils/createMultimap m))

(defn create-entity
  "Creates a new Entity from a DTO map"
  [api-key new-dto host-url]
  (let [entity (-> (ctx api-key)
                   (.insertEntity
                     (-> new-dto
                         (update-in [:links] create-multimap))))]

    (into-map-array (seq [entity]) host-url)))


(defn update-entity [api-key id dto host-url]
  (let [entity     (-> (ctx api-key) (.getObjectWithUUID (parse-uuid id)))]
    (.update entity (update-in dto [:links] create-multimap))
    (into-map-array [entity] host-url)
    ))

(defn delete-entity [api_key id]
  (let [entity (-> (ctx api_key) (. getObjectWithUuid (parse-uuid id)))
        trash_resp (-> (ctx api_key) (. trash entity) (.get))]

    {:success (not (empty? trash_resp))}))

(defn index-resource [api-key resource host-url]
  (let [resources (case resource
                    "project" (-> (ctx api-key) (.getProjects))
                    "source" (-> (ctx api-key) (.getTopLevelSources))
                    "protocol" (-> (ctx api-key) (.getProtocols))
                    )]

    (into-map-array resources host-url)))

