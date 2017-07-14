(ns ovation.util
  (:import (java.net URI)
           (java.util UUID Date))
  (:require [clojure.string :refer [join]]
            [ovation.version :refer [version]]
            [clojure.walk :as walk]
            [clojure.data.json :as json]
            [clojure.string :as s]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [clojure.core.async :refer [<!! <! go-loop] :as async]
            [slingshot.slingshot :refer [throw+]]
            [clojure.tools.logging :as logging]))

(def RELATION_TYPE "Relation")

(defn make-uuid
  "Wraps java.util.UUID/randomUUID for test mocking."
  []
  (UUID/randomUUID))

(defn parse-uuid [s]
  (if (nil? (re-find #"\w{8}-\w{4}-\w{4}-\w{4}-\w{12}" s))
    (let [buffer (java.nio.ByteBuffer/wrap
                   (javax.xml.bind.DatatypeConverter/parseHexBinary s))]
      (UUID. (.getLong buffer) (.getLong buffer)))
    (UUID/fromString s)))



(defn entity-type-name
  [doc]
  (s/lower-case (:type doc)))

(defn entity-type-keyword
  [doc]
  (keyword (entity-type-name doc)))

(defn entity-type-name-keyword
  [name]
  (keyword (s/lower-case name)))

(defn into-id-map
  "Converts a mappable collection of documents into a map {:_id => doc}"
  [docs]
  (into {} (map (fn [doc] [(:_id doc) doc]) docs)))

(defn entity-id
  "The entity ID for a given URI"
  [uri]
  (get (clojure.string/split uri #"/") 3))


(defn create-uri [id]
  "Creates an ovation URI from string id"
  (if (instance? URI id)
    id
    (URI. (format "ovation://entities/%s" id))))

(defn format-iso8601
  "Formats the date instance as an ISO-8601 string"
  [d]
  (tf/unparse (tf/formatters :date-hour-minute-second-ms) d))

(defn jsonify-value
  "JSON-ifies Date and UUIDs, leaving everything else as-is"
  [v]
  (condp instance? v
    Date (format-iso8601 v)
    UUID (str v)
    v))

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

(defn remove-leading-slash
  [path]
  (if (.startsWith path "/")
    (.substring path 1)
    path))

(defn prefixed-path
  [p]
  (let [path (-> p
               (s/lower-case)
               (s/trim))
        prefix (join-path ["" "api" version])]
    (if (.startsWith path prefix)
      path
      (join-path [prefix (remove-leading-slash path)]))))

(defn iso-now
  "Gets the ISO date time string for (t/now)"
  []
  (format-iso8601 (t/now)))

(defn iso-short-now
  "Gets the short ISO date time string for (t/now)"
  []
  (tf/unparse (tf/formatters :date-time) (t/now)))

(defn filter-type
  [entity-type docs]
  (filter #(= entity-type (:type %)) docs))


(defn write-json-body
  [body]
  (json/write-str (walk/stringify-keys body)))

(defn remove-nil-values
  "Remove nil values from record"
  [record]
  (apply dissoc
    record
    (for [[k v] record :when (nil? v)] k)))

;; Async utilities

(defn exception?
  "Tests response for Throwable or :ring.util.http-response/response"
  [response]
  (or
    (instance? Throwable response)
    (#{(str :ring.util.http-response/response) :ring.util.http-response/response} (:type response))))

(defn <??
  "Async pop that throws an exception if item returned is throwable. Returns nil on timeout.
   Timeout in milliseconds (default 1000ms).
   This function comes from David Nolen."
  [c]
  (let [returned (<!! c)]
    (if (exception? returned)
      (throw+ returned)
      returned)))


