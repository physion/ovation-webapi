(ns ovation.util
  (:import (java.net URI)
           (java.util UUID))
  (:require [clojure.string :refer [join]]
            [ovation.version :refer [version]]
            [clojure.walk :as walk]
            [clojure.data.json :as json]
            [clojure.string :as s]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [clojure.core.async :refer [<!!] :as async]))

(def RELATION_TYPE "Relation")

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
  (tf/unparse (tf/formatters :date-hour-minute-second-ms) (t/now)))

(defn iso-short-now
  "Gets the short ISO dat time string for (t/now)"
  []
  (tf/unparse (tf/formatters :date-time) (t/now)))

(defn filter-type
  [entity-type docs]
  (filter #(= entity-type (:type %)) docs))


;; Async utilities

(defn ncpus []
  (.availableProcessors (Runtime/getRuntime)))

(defn <??
  "Async pop that throws an exception if item returned is throwable
   This function comes from David Nolen"
  [c]
  (let [returned (<!! c)]
    (if (instance? Throwable returned)
      (throw returned)
      returned)))

(def default-parallelism (+ (ncpus) 1))

(defn pipeline
  [in xf & {:keys [parallelism buffer] :or {parallelism default-parallelism
                                            buffer      16}}]
  (let [out (async/chan (async/buffer buffer))]
    (async/pipeline parallelism out xf in)
    out))

