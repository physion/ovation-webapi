(ns ovation.transform.write
  (:require [ovation.version :refer [version version-path]]
            [ovation.version :as ver]
            [ovation.util :as util]
            [clojure.tools.logging :as logging]
            [clj-time.core :as t]
            [clj-time.format :as f]))

(defn ensure-id
  "Makes sure there's an _id for entity"
  [doc]
  (if (nil? (:_id doc))
    (assoc doc :_id (util/make-uuid))
    doc))

(defn add-api-version
  "Insert API version"
  [doc]
  (assoc doc :api_version ver/schema-version))

(defn ensure-owner
  "Adds owner reference"
  [doc owner-id]
  (if (nil? (:owner doc))
    (assoc doc :owner owner-id)
    doc))


(defn add-collaboration-roots
  [doc roots]
  (if roots
    (assoc-in doc [:links :_collaboration_roots] roots)
    doc))

(defn ensure-created-at
  [doc timestamp]
  (if (not (get-in doc [:attributes :created-at]))
    (assoc-in doc [:attributes :created-at] timestamp)
    doc))

(defn add-updated-at
  [doc timestamp]
  (assoc-in doc [:attributes :updated-at] timestamp))

(defn doc-to-couch
  [owner-id collaboration-roots doc]
  (if (and (:type doc) (not (= (str (:type doc)) util/RELATION_TYPE)))
    (let [time (f/unparse (f/formatters :date-time) (t/now))
          roots (or collaboration-roots (get-in doc [:links :_collaboration_roots] []))]
      (-> doc
        ensure-id
        (ensure-created-at time)
        add-api-version
        (ensure-owner owner-id)
        (add-updated-at time)
        (dissoc :links)
        (dissoc :relationships)
        (dissoc :permissions)
        (add-collaboration-roots roots)))
    doc))

(defn to-couch
  "Transform documents for CouchDB"
  [owner-id docs & {:keys [collaboration_roots] :or {collaboration_roots nil}}]
  (logging/info "Transforming to couch" docs)
  (map #(doc-to-couch owner-id collaboration_roots %) docs))
