(ns ovation.links
  (:require [ovation.util :refer [create-uri]]
            [ovation.couch :as couch]
            [ovation.util :as util]
            [ovation.auth :as auth]
            [ovation.core :as core])
  (:import (us.physion.ovation.data EntityDao$Views)))



;; QUERY
(defn- eq-doc-label
  [label]
  (fn [doc] (if-let [doc-label (get-in doc [:attributes :label])]
              (= label doc-label)
              false)))

(defn get-link-targets
  "Gets the document targets for id--rel->"
  [db id rel & {:keys [label name] :or {label nil name nil}}]
  (let [opts {:key    (if name [id rel name] [id rel])
              :reduce false :include_docs true}
        docs (map :doc (couch/get-view db EntityDao$Views/LINKS opts))]
    (if label
      (filter (eq-doc-label label) docs)
      docs)))




;; COMMAND
(defn- link-id
  [source-id rel target-id & {:keys [name] :or [name nil]}]
  (if name
    (format "%s--%s>%s-->%s" source-id rel name target-id)
    (format "%s--%s-->%s" source-id rel target-id)))

(defn- link-path
  [source-id rel name]
  (if name
    (util/join-path ["" "entities" source-id "named_links" (clojure.core/name rel) (clojure.core/name name)])
    (util/join-path ["" "entities" source-id "links" (clojure.core/name rel)])))

(defn- collaboration-roots
  [doc]
  (get-in doc [:links :_collaboration_roots] []))

(defn- add-roots
  [doc roots]
  (let [current (collaboration-roots doc)]
    (assoc-in doc [:links :_collaboration_roots] (concat current roots))))

(defn- update-collaboration-roots-for-target
  [source target]
  (let [source-roots (collaboration-roots source)
        source-type (clojure.string/lower-case (:type source))
        target-roots (collaboration-roots target)
        target-type (clojure.string/lower-case (:type target))]
    (cond
      (= source-type "project") [(add-roots target source-roots) source]
      (= target-type "project") [(add-roots source target-roots)]


    (= source-type "folder") [(add-roots target source-roots) source]
    (= target-type "folder") [(add-roots source target-roots)]

    (= source-type "experiment") [(add-roots target source-roots) source]
    (= target-type "experiment") [(add-roots source target-roots)]

    (= target-type "source") [(add-roots target source-roots) source]

    (= target-type "protocol") [(add-roots target source-roots)]

    :else
    [source]
    )) )

(defn add-link
  [auth doc id rel target-id & {:keys [inverse-rel name] :or [inverse-rel nil
                                                              name nil]}]

  (auth/check! id :auth/update doc)
  (let [doc-id (:_id doc)
        base {:_id       (link-id doc-id rel target-id :name name)
              :target_id target-id
              :source_id doc-id
              :rel       rel
              :user_id   id}
        named (if name (assoc base :name name) base)
        link (if inverse-rel (assoc named :inverse_rel inverse-rel) named)
        path (link-path doc-id rel name)
        linked-doc (if name
                     (assoc-in doc [:named_links (keyword rel) (keyword name)] path)
                     (assoc-in doc [:links (keyword rel)] path))
        updated-docs (update-collaboration-roots-for-target linked-doc (first (core/get-entities auth [target-id])))]
    (conj updated-docs link)))

(defn delete-link
  [auth doc id rel target-id & {:keys [inverse-rel name] :or [inverse-rel nil
                                                         name nil]}]
  (auth/check! id :auth/update doc)
  (let [link-id (link-id (:_id doc) rel target-id :name name)
        db (couch/db auth)
        links (couch/all-docs db [link-id])]
    (couch/delete-docs db links)))
