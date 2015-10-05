(ns ovation.transform.write
  (:require [ovation.version :refer [version version-path]]
            [ovation.version :as ver]
            [ovation.util :as util]
            [clojure.tools.logging :as logging]))

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

(defn add-owner
  "Adds owner reference"
  [doc owner-id]
  (assoc doc :owner owner-id))


(defn add-collaboration-roots
  [doc roots]
  (if roots
    (assoc-in doc [:links :_collaboration_roots] roots)
    doc))

(defn doc-to-couch
  [owner-id collaboration-roots doc]
  (if (and (:type doc) (not (= (str (:type doc)) util/RELATION_TYPE)))
    (-> doc
      ensure-id
      add-api-version
      (add-owner owner-id)
      (dissoc :links)
      (dissoc :relationships)
      (add-collaboration-roots collaboration-roots))
    doc))

(defn to-couch
  "Transform documents for CouchDB"
  [owner-id docs & {:keys [collaboration_roots] :or {collaboration_roots nil}}]
  (logging/info "Transforming to couch" docs)
  (map #(doc-to-couch owner-id collaboration_roots %) docs))
