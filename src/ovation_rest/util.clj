(ns ovation-rest.util
  (:import (java.net URI)
           (us.physion.ovation.domain URIs))
  (:require [clojure.pprint]
            [ovation-rest.context :as context]
            )
  )

(defn ctx [api_key]
  (context/cached-context api_key))

(defn get-body-from-request [request]
  (slurp (:body request)))

(defn object-to-json [obj]
  (->
    (new com.fasterxml.jackson.databind.ObjectMapper)
    (.registerModule (com.fasterxml.jackson.datatype.guava.GuavaModule.))
    (.registerModule (com.fasterxml.jackson.datatype.joda.JodaModule.))
    (.configure com.fasterxml.jackson.databind.SerializationFeature/WRITE_DATES_AS_TIMESTAMPS false)
    (.writeValueAsString obj)
    )
  )

(defn json-to-map [json]
  (->
    (new com.fasterxml.jackson.databind.ObjectMapper)
    (.registerModule (com.fasterxml.jackson.datatype.guava.GuavaModule.))
    (.registerModule (com.fasterxml.jackson.datatype.joda.JodaModule.))
    (.readValue json java.util.Map)
    )
  )


(defn entity-to-dto
  "Clojure wrapper for entity.toMap()"
  [entity]
  (.toMap entity))

(defn augment-entity-dto [dto]
  (let [links (.get dto "links")]
    (.put links "self" (str (URIs/create dto)))
    (.put dto "links" links)
    dto))

(defn convert-entity-to-map
  "Converts an entity to a map suitable for response (e.g. adds additional links=>self)"
  [entity]
  (augment-entity-dto (entity-to-dto entity)))

(defn entities-to-json [entity_seq]
  (object-to-json (into-array (map convert-entity-to-map entity_seq)))
  )

(defn host-from-request [request]
  (let [
         scheme (clojure.string/join "" [(name (get request :scheme)) "://"])
         host (get (get request :headers) "host")
         ]
    (clojure.string/join "" [scheme host "/"])
    )
  )

(defn munge-strings [s host]
  (.replaceAll (new String s) "ovation://" host))

(defn unmunge-strings [s host]
  (clojure.pprint/pprint (.getClass s))
  (.replaceAll (new String s) host "ovation://"))

(defn auth-filter-middleware [request handler-fn]
  (let [params (get request :query-params)
        status (if-not (contains? params "api-key")
                 (num 401)
                 (num 200))
        body (if (= 200 status)
               (str (handler-fn (get params "api-key")))
               (str "Please log in to access resource"))]

    {:status       status
     :body         (munge-strings body (host-from-request request))
     :content-type "application/json"}
    )
  )

(defn parse-uuid [s]
  (if (nil? (re-find #"\w{8}-\w{4}-\w{4}-\w{4}-\w{12}" s))
    (let [buffer (java.nio.ByteBuffer/wrap
                   (javax.xml.bind.DatatypeConverter/parseHexBinary s))]
      (java.util.UUID. (.getLong buffer) (.getLong buffer))
      )
    (java.util.UUID/fromString s)
    )
  )
