(ns ovation.util
  (:import (java.net URI)
           (us.physion.ovation.domain URIs))
  (:require [ovation.context :as context]
            [ovation.interop :as interop]
            [ovation.paths :as paths]
            [clojure.string :refer [join]]
            [clojurewerkz.urly.core :as urly]
            [pathetic.core :refer [url-normalize up-dir]]
            [ovation.version :refer [version-path]]))

(defn ctx [api-key]
  (context/cached-context api-key))

(defn parse-uuid [s]
  (if (nil? (re-find #"\w{8}-\w{4}-\w{4}-\w{4}-\w{12}" s))
    (let [buffer (java.nio.ByteBuffer/wrap
                   (javax.xml.bind.DatatypeConverter/parseHexBinary s))]
      (java.util.UUID. (.getLong buffer) (.getLong buffer)))
    (java.util.UUID/fromString s)))


(defn get-body-from-request [request]
  (slurp (:body request)))


(defn- split-query [u]
  (clojure.string/split u #"\?" 2))


(defn get-entity-id
  "The entity ID for a given URI"
  [uri]
  (get (clojure.string/split uri #"/") 3))


(defn create-uri [id]
  "Creates an ovation URI from string id"
  (if (instance? URI id)
    id
    (URIs/create id)))


(defn host-from-request [request]
   (let [scheme (clojure.string/join "" [(name (get request :scheme)) "://"])
         host (get (get request :headers) "host")]
     (clojure.string/join "" [scheme host "/"])))


(defn ovation-query
  [request]
  (let [params (:query-params request)]
    (join "&" (for [[k v] (select-keys params (for [[k v] params :when (not (= k "api-key"))] k))] (format "%s=%s" k v)))))

