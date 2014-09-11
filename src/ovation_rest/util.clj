(ns ovation-rest.util
  (:import (java.net URI)
           (us.physion.ovation.domain URIs))
  (:require [clojure.pprint]
            [ovation-rest.context :as context]
            [ovation-rest.interop :as interop]
            [clojurewerkz.urly.core :as urly]
            )
  )

(defn ctx [api-key]
  (context/cached-context api-key))

(defn entity-to-dto
  "Clojure wrapper for entity.toMap()"
  [entity]
  (interop/clojurify (.toMap entity)))

(defn entity-uri [dto]
  "Constructs an ovation://entities/... URI string from a clojure dto"
  (format "ovation://entities/%s" (:_id dto)))


(defn augment-entity-dto [dto]
  "Augment an entity dto with the links.self reference"
  (merge-with conj dto {:links {:self #{(entity-uri dto)}}}))

(defn convert-entity-to-map
  "Converts an entity to a map suitable for response (e.g. adds additional links=>self)"
  [entity]
  (augment-entity-dto (entity-to-dto entity)))

(defn into-map-array [entity_seq]
  "Converts a seq of entities into an array of Maps"
  (into-array (map convert-entity-to-map entity_seq)))

(defn host-from-request [request]
  (let [
         scheme (clojure.string/join "" [(name (get request :scheme)) "://"])
         host (get (get request :headers) "host")
         ]
    (clojure.string/join "" [scheme host "/"])))


(defn parse-uuid [s]
  (if (nil? (re-find #"\w{8}-\w{4}-\w{4}-\w{4}-\w{12}" s))
    (let [buffer (java.nio.ByteBuffer/wrap
                   (javax.xml.bind.DatatypeConverter/parseHexBinary s))]
      (java.util.UUID. (.getLong buffer) (.getLong buffer)))
    (java.util.UUID/fromString s)))

(defn- split-query [u]
  (clojure.string/split u #"\?" 2))

(defn to-web-uri [entity-uri base-uri]
  "Converts an ovation:// URI to a web (http[s]://) URI for the given server base URI"
  (let [entity-urly (urly/url-like entity-uri)
        q (urly/query-of entity-uri)
        result-base (urly/resolve base-uri (clojure.string/join "/" [(urly/host-of entity-urly) (urly/path-of entity-urly)]))]

    (if q
      (format "%s?%s" result-base q)
      result-base)))

(defn to-ovation-uri [web-uri base-uri]
  "Converts a web URI with the given server base to an ovation:// URI"
  (let [[web-base q] (split-query web-uri)
        web-urly (urly/url-like web-base)
        base-urly (urly/url-like (urly/normalize-url base-uri))
        part (.relativize (-> base-urly (.toURL) (.toURI)) (-> web-urly (.toURL) (.toURI)))
        result-base (urly/normalize-url (format "ovation://%s" part))]

    (if q
      (format "%s?%s" result-base q)
      result-base)))
