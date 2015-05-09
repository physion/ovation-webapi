(ns ovation.util
  (:import (java.net URI)
           (us.physion.ovation.domain URIs)
           (java.util UUID))
  (:require [clojure.string :refer [join]]
            [ovation.version :refer [version-path]]
            [clojure.walk :as walk]
            [clojure.data.json :as json]))


(defn make-uuid
  "Wraps java.util.UUID/randomUUID for test mocking."
  []
  (java.util.UUID/randomUUID))

(defn parse-uuid [s]
  (if (nil? (re-find #"\w{8}-\w{4}-\w{4}-\w{4}-\w{12}" s))
    (let [buffer (java.nio.ByteBuffer/wrap
                   (javax.xml.bind.DatatypeConverter/parseHexBinary s))]
      (UUID. (.getLong buffer) (.getLong buffer)))
    (UUID/fromString s)))


(defn get-entity-id
  "The entity ID for a given URI"
  [uri]
  (get (clojure.string/split uri #"/") 3))


(defn create-uri [id]
  "Creates an ovation URI from string id"
  (if (instance? URI id)
    id
    (URIs/create id)))


(defn to-json
  "Converts a keywordized map to json string"
  [m]
  (json/write-str (walk/stringify-keys m)))

(defn from-json
  "Converts a json string to keywordized map"
  [s]

  (walk/keywordize-keys (json/read-str s)))


(defn join-path
  [comps]
  (clojure.string/join "/" comps))

