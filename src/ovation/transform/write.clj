(ns ovation.transform.write
  (:require [ovation.version :refer [version version-path]]
            [ovation.version :as ver]
            [ovation.util :as util]))

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
  (assoc-in doc [:links :_collaboration_roots] roots))

(defn link-ov
  [id rel]
  (format "ovation://views/links?key=[%%22ovation://entities/%s%%22,%%22%s%%22]" id (clojure.core/name rel)))

(defn named-link-ov
  [rel id name]
  (format "ovation://views/links?key=[%%22ovation://entities/%s%%22,%%22%s%%22,%%22%s%%22]" id (clojure.core/name rel) (clojure.core/name name)))

(defn make-ov-links
  [id links link-path-fn]
  (into {} (map (fn [x] (let [rel (first x)]
                          [rel {:uri (link-path-fn id rel)}])) links)))

(defn links-to-ov
  [doc]
  (let [links (make-ov-links (:_id doc) (:links doc) link-ov)
        named-links (into {} (map (fn [x]
                                    (let [rel (first x)
                                          m (second x)]
                                      [rel (make-ov-links (:_id doc) m (partial named-link-ov rel))]))) (:named_links doc))
        ]
    (-> doc
      (assoc-in [:links] links)
      (assoc-in [:named_links] named-links))))

(defn doc-to-couch
  [owner-id collaboration-roots doc]
  (-> doc
    ensure-id
    add-api-version
    (add-owner owner-id)
    (add-collaboration-roots collaboration-roots)))         ;TODO remove/transform links?

(defn to-couch
  "Transform documents for CouchDB"
  [owner-id docs & {:keys [collaboration_roots] :or {collaboration_roots nil}}]
  (map (partial doc-to-couch owner-id collaboration_roots) docs))
