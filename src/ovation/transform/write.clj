(ns ovation.transform.write
  (:require [ovation.version :refer [version version-path]]
            [ovation.version :as ver]
            [ovation.util :as util]
            [ovation.request-context :as request-context]
            [clojure.tools.logging :as logging]
            [com.climate.newrelic.trace :refer [defn-traced]]))

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
  (if owner-id
    (if (nil? (:owner doc))
      (assoc doc :owner owner-id)
      doc)
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

(defn add-organization
  [doc ctx]
  (assoc doc :organization (::request-context/org ctx)))

(defn-traced doc-to-couch
  [ctx collaboration-roots doc]
  (let [owner-id (request-context/user-id ctx)]
    (if (and (:type doc) (not (= (str (:type doc)) util/RELATION_TYPE)))
      (let [time  (util/iso-short-now)
            roots (or collaboration-roots (get-in doc [:links :_collaboration_roots] []))]
        (-> doc
          ensure-id
          (ensure-created-at time)
          add-api-version
          (ensure-owner owner-id)
          (add-updated-at time)
          (add-organization ctx)
          (dissoc :organization_id)
          (dissoc :links)
          (dissoc :relationships)
          (dissoc :permissions)
          (add-collaboration-roots roots)))
      doc)))

(defn-traced to-couch
  "Transform documents for CouchDB"
  [ctx docs & {:keys [collaboration_roots] :or {collaboration_roots nil}}]
  (logging/info "Transforming to couch" docs)
  (map #(doc-to-couch ctx collaboration_roots %) docs))
