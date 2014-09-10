(ns ovation-rest.util
  (:import (java.net URI)
           (us.physion.ovation.domain URIs))
  (:require [clojure.pprint]
            [ovation-rest.context :as context]
            [ovation-rest.interop :as interop]
            [clojurewerkz.urly.core :refer [url-like host-of path-of query-of resolve mutate-query normalize-url]]
            )
  )

(defn ctx [api_key]
  (context/cached-context api_key))

(defn get-body-from-request [request]
  (slurp (:body request)))


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

(defn munge-strings [s host]
  (.replaceAll (new String s) "ovation://" host))           ;; TODO munge only primary URIs (no query parameters) in links, named_links, notes, properties

(defn unmunge-strings [s host]
  (clojure.pprint/pprint (.getClass s))
  (.replaceAll (new String s) host "ovation://"))           ;; TODO unmunge only primary URIs (no query parameters) in links, named_links, notes, properties

(defn auth-filter-middleware [request handler-fn]
  (let [params (get request :query-params)
        status (if-not (contains? params "api-key")
                 (num 401)
                 (num 200))
        body (if (= 200 status)
               (str (handler-fn (get params "api-key")))
               (str "Please log in to access resource"))]

    {:status       status
     :body         (munge-strings body (host-from-request request)) ; TODO return map here. defapi automatically adds json handling
     :content-type "application/json"}))

(defn parse-uuid [s]
  (if (nil? (re-find #"\w{8}-\w{4}-\w{4}-\w{4}-\w{12}" s))
    (let [buffer (java.nio.ByteBuffer/wrap
                   (javax.xml.bind.DatatypeConverter/parseHexBinary s))]
      (java.util.UUID. (.getLong buffer) (.getLong buffer)))
    (java.util.UUID/fromString s)))

(defn to-web-uri [entity-uri base-uri]
  "Converts an ovation:// URI to a web (http[s]://) URI for the given server base URI"
  (let [entity-urly (url-like entity-uri)]

    (normalize-url (mutate-query (resolve base-uri (clojure.string/join "/" [(host-of entity-urly) (path-of entity-urly)]))
                                 (query-of entity-urly)))))

(defn to-ovation-uri [web-uri base-uri]
  "Converts a web URI with the given server base to an ovation:// URI"
  (let [web-urly (url-like web-uri)
        base-urly (url-like (normalize-url base-uri))
        part (.relativize (-> base-urly (.toURL) (.toURI)) (-> web-urly (.toURL) (.toURI)))]

    (normalize-url (format "ovation://%s" part))))
