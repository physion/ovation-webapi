(ns ovation-rest.entity
  (:import (us.physion.ovation.domain URIs))
  (:require [clojure.walk :refer [stringify-keys]]
            [ovation-rest.util :refer :all]))

(defn- api-key
  "Extracts the API key from request query parameters"
  [request]
  ("api-key" (:query-params request)))

;(defn json-to-map [json]
;  (->
;    (new com.fasterxml.jackson.databind.ObjectMapper)
;    (.registerModule (com.fasterxml.jackson.datatype.guava.GuavaModule.))
;    (.registerModule (com.fasterxml.jackson.datatype.joda.JodaModule.))
;    (.readValue json java.util.Map)
;    )
;  )

(defn get-entity
  "Gets a single entity by ID (uuid)"
  [api-key id]
  (into-seq (seq [(-> (ctx api-key) (.getObjectWithUuid (parse-uuid id)))])))

(defn create-multimap [m]
  (us.physion.ovation.util.MultimapUtils/createMultimap m))

(defn create-entity [api-key new-dto]
  "Creates a new Entity from a DTO map"
    (into-seq (seq [(-> (ctx api-key) (.insertEntity (stringify-keys new-dto)))])))

(defn get-link [api-key id rel]
  "Returns all entities from entity(id)->rel and returns them"
  (into-seq (into [] (.getEntities (-> (ctx api-key) (.getObjectWithUuid (parse-uuid id))) rel))))

(defn create-link [api-key id rel target inverse]
  "Creates a new link from entity(id) -> entity(target)"
  (let [entity (-> (ctx api-key) (.getObjectWithUuid (parse-uuid id)))
        linked (.addLink entity rel (create-uri target) inverse)]
    (into-seq (seq [linked]))))

(defn delete-link [api-key id rel target]
  "Deletes a named link on entity(id)"
  (let [entity (-> (ctx api-key) (. getObjectWithUuid (parse-uuid id)))
        delete (.removeLink entity rel (create-uri target))]
  {:success (not (empty? delete))}))

(defn get-named-link [api-key id rel named inverse]
  "Returns all entities from entity(id)->link"
  (into-seq (into [] (.getNamedEntities (-> (ctx api-key) (.getObjectWithUuid (parse-uuid id))) rel named))))

(defn create-named-link [api-key id rel target inverse]
  "Creates a new link from entity(id) -> entity(target)"
  (let [entity ((ctx api-key) (.getObjectWithUuid (parse-uuid id)))
        linked (.addNamedLink entity rel (create-uri target) inverse)]
    (into-seq (seq [linked]))))

(defn delete-named-link [api-key id rel named target]
  "Deletes a named link on entity(id)"
  (let [entity (-> (ctx api-key) (. getObjectWithUuid (parse-uuid id)))
        delete (.removeNamedLink entity rel named (create-uri target))]
  {:success (not (empty? delete))}))

(defn get-annotations [api-key id]
  "Returns all annotations associated with entity(id)"
  (into [] (.getAnnotations (-> (ctx api-key) (.getObjectWithUuid (parse-uuid id))))))

(defn update-entity [api-key id dto]
  (let [entity     (-> (ctx api-key) (.getObjectWithUuid (parse-uuid id)))]
    (.update entity (stringify-keys (update-in dto [:links] create-multimap)))
    (into-seq [entity])
    ))

(defn delete-entity [api-key id]
  (let [entity (-> (ctx api-key) (. getObjectWithUuid (parse-uuid id)))
        trash_resp (-> (ctx api-key) (. trash entity) (.get))]

    {:success (not (empty? trash_resp))}))

(defn- ^{:testable true} get-projects [ctx]
  (.getProjects ctx))

(defn index-resource [api-key resource]
  (let [resources (case resource
                    "projects" (get-projects (ctx api-key))
                    "sources" (-> (ctx api-key) (.getTopLevelSources))
                    "protocols" (-> (ctx api-key) (.getProtocols)))] 
    (into-seq resources)))

(defn- get-view-results [ctx uri]
  (.getObjectsWithURI ctx (clojure.string/replace uri "\"" "%22")))

(defn get-view [api-key full-url host-url]
  (into-seq (get-view-results (ctx api-key) full-url)))

