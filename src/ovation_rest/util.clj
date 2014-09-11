(ns ovation-rest.util
  (:import (java.net URI)
           (us.physion.ovation.domain URIs))
  (:require [clojure.pprint]
            [ovation-rest.context :as context]
            [ovation-rest.interop :as interop]
            [ovation-rest.paths :as paths]
            [clojure.string :refer [join]]
            [clojurewerkz.urly.core :as urly]
            [pathetic.core :refer [url-normalize up-dir]]))

(defn ctx [api-key]
  (context/cached-context api-key))

(defn get-body-from-request [request]
  (slurp (:body request)))

(defn- split-query [u]
  (clojure.string/split u #"\?" 2))

(defn to-web-uri
  "Converts an ovation:// URI to a web (http[s]://) URI for the given server base URI"
  [base_uri entity_uri]
  (let [entity_urly (urly/url-like entity_uri)
        q (urly/query-of entity_uri)
        result_base (urly/resolve base_uri (clojure.string/join "/" [(urly/host-of entity_urly) (urly/path-of entity_urly)]))]

    (if q
      (format "%s?%s" result_base q)
      result_base)))

(defn to-ovation-uri
  "Converts a web URI with the given server base to an ovation:// URI"
  [web_uri base_uri]
  (let [[web_base q] (split-query web_uri)
        web_urly (urly/url-like web_base)
        base_urly (urly/url-like (urly/normalize-url base_uri))
        part (.relativize (-> base_urly (.toURL) (.toURI)) (-> web_urly (.toURL) (.toURI)))
        result_base  (format "ovation://%s" (str part))]

    (if q
      (format "%s?%s" result_base q)
      result_base)))

(defn host-from-request [request]
   (let [scheme (clojure.string/join "" [(name (get request :scheme)) "://"])
         host (get (get request :headers) "host")]
     (clojure.string/join "" [scheme host "/"])))

(defn entity-to-dto
  "Clojure wrapper for entity.toMap()"
  [entity]
  (interop/clojurify (.toMap entity)))

(defn entity-uri
  "Constructs an ovation://entities/... URI string from a clojure dto"
  [dto]
  (format "ovation://entities/%s" (:_id dto)))

(defn augment-entity-dto
  "Augment an entity dto with the links.self reference"
  [dto base_uri]
  (let [add_self         (merge-with conj dto {:links {:self #{(entity-uri dto)}}})
        new_links_map    (into {} (map (fn [x] [(first x) (set (map (fn [y] (to-web-uri base_uri y)) (second x)))]) (add_self :links)))]
    (assoc-in add_self [:links] new_links_map)))

(defn convert-entity-to-map
  "Converts an entity to a map suitable for response (e.g. adds additional links=>self)"
  [base_uri entity]
  (augment-entity-dto (entity-to-dto entity) base_uri))

(defn into-seq
  "Converts a seq of entities into an array of Maps"
  [entity_seq base_uri]
  (seq (into-array (map (partial convert-entity-to-map base_uri) entity_seq))))


(defn parse-uuid [s]
  (if (nil? (re-find #"\w{8}-\w{4}-\w{4}-\w{4}-\w{12}" s))
    (let [buffer (java.nio.ByteBuffer/wrap
                   (javax.xml.bind.DatatypeConverter/parseHexBinary s))]
      (java.util.UUID. (.getLong buffer) (.getLong buffer)))
    (java.util.UUID/fromString s)))

(defn- request-context
  [request]
  (:context request))

(defn host-context
  "Calculates the host context for a given request. The host context is the host (e.g. https://server.com/)
  plus the context path (e.g. /api/v1). If :remove-levels is provided, the given number of levels are removed
  from the context path. For example, :remove-levels 1 would give https://server.com/api instead of
  https://server.com/api/v1"

  [request & {:keys [remove-levels] :or {:remove-levels 0}}]
  (let [host-url (host-from-request request)
        context-vec (paths/split (request-context request))
        truncated-vec (if remove-levels (vec (take (- (alength (into-array context-vec)) remove-levels) context-vec))
                                        context-vec)]
    (url-normalize (paths/join [host-url (paths/join truncated-vec)]))))


(defn ovation-query
  [request]
  (let [params (:query-params request)]
    (join "&" (for [[k v] (select-keys params (for [[k v] params :when (not (= k "api-key"))] k))] (format "%s=%s" k v)))))
