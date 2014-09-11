(ns ovation-rest.util
  (:import (java.net URI)
           (us.physion.ovation.domain URIs))
  (:require [clojure.pprint]
            [ovation-rest.context :as context]
            [ovation-rest.interop :as interop]
            [clojurewerkz.urly.core :as urly]
            )
  )

(defn ctx [api_key]
  (context/cached-context api_key))

(defn get-body-from-request [request]
  (slurp (:body request)))

(defn- split-query [u]
  (clojure.string/split u #"\?" 2))

(defn to-web-uri [base_uri entity_uri]
  "Converts an ovation:// URI to a web (http[s]://) URI for the given server base URI"
  (let [entity_urly (urly/url-like entity_uri)
        q (urly/query-of entity_uri)
        result_base (urly/resolve base_uri (clojure.string/join "/" [(urly/host-of entity_urly) (urly/path-of entity_urly)]))]
;        yo (clojure.pprint/pprint result_base)]

    (if q
      (format "%s?%s" result_base q)
      result_base)))

(defn to-ovation-uri [web_uri base_uri]
  "Converts a web URI with the given server base to an ovation:// URI"
  (let [[web_base q] (split-query web_uri)
        web_urly (urly/url-like web_base)
        base_urly (urly/url-like (urly/normalize-url base_uri))
        part (.relativize (-> base_urly (.toURL) (.toURI)) (-> web_urly (.toURL) (.toURI)))
        result_base (urly/normalize-url (format "ovation://%s" part))]

    (if q
      (format "%s?%s" result_base q)
      result_base)))

(defn host-from-request [request]
  (let [scheme (clojure.string/join "" [(name (get request :scheme)) "://"])
        host   (clojure.string/join "" [(get (get request :headers) "host" "/")])]
    (clojure.string/join "" [scheme host "/"])))

(defn entity-to-dto
  "Clojure wrapper for entity.toMap()"
  [entity]
  (interop/clojurify (.toMap entity)))

(defn entity-uri [dto]
  "Constructs an ovation://entities/... URI string from a clojure dto"
  (format "ovation://entities/%s" (:_id dto)))

(defn augment-entity-dto [dto base_uri]
  "Augment an entity dto with the links.self reference"
  (let [add_self         (merge-with conj dto {:links {:self #{(entity-uri dto)}}})
        new_links_map    (into {} (map (fn [x] [(first x) (set (map (fn [y] (to-web-uri base_uri y)) (second x)))]) (add_self :links)))]
    (assoc-in add_self [:links] new_links_map)))

(defn convert-entity-to-map
  "Converts an entity to a map suitable for response (e.g. adds additional links=>self)"
  [base_uri entity]
  (augment-entity-dto (entity-to-dto entity) base_uri))

(defn into-seq [entity_seq base_uri]
  "Converts a seq of entities into an array of Maps"
  (seq (into-array (map (partial convert-entity-to-map base_uri) entity_seq))))

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

    body))

(defn parse-uuid [s]
  (if (nil? (re-find #"\w{8}-\w{4}-\w{4}-\w{4}-\w{12}" s))
    (let [buffer (java.nio.ByteBuffer/wrap
                   (javax.xml.bind.DatatypeConverter/parseHexBinary s))]
      (java.util.UUID. (.getLong buffer) (.getLong buffer)))
    (java.util.UUID/fromString s)))

