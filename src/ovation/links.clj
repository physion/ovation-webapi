(ns ovation.links
  (:require [ovation.util :refer [create-uri]]
            [ovation.couch :as couch]
            [ovation.util :as util]
            [ovation.auth :as auth]
            [ovation.core :as core]
            [slingshot.slingshot :refer [throw+]]
            [clojure.set :refer [union]]
            [ovation.constants :as k]
            [ovation.routes :as r]
            [ovation.transform.read :as tr]))


;; QUERY
(defn- eq-doc-label
  [label]
  (fn [doc] (if-let [doc-label (get-in doc [:attributes :label])]
              (= label doc-label)
              false)))

(defn get-links
  [auth id rel routes & {:keys [label name] :or {label nil name nil}}]
  (let [db (couch/db auth)
        opts {:startkey      (if name [id rel name] [id rel])
              :endkey        (if name [id rel name] [id rel])
              :inclusive_end true
              :reduce        false :include_docs true}]
    (tr/values-from-couch
      (couch/get-view db k/LINK-DOCS-VIEW opts)
      routes)))

(defn get-link-targets
  "Gets the document targets for id--rel->"
  [auth id rel routes & {:keys [label name] :or {label nil name nil}}]
  (let [db (couch/db auth)
        opts {:startkey      (if name [id rel name] [id rel])
              :endkey        (if name [id rel name] [id rel])
              :inclusive_end true
              :reduce        false :include_docs true}]
    (tr/entities-from-couch (if label
                     (filter (eq-doc-label label) (couch/get-view db k/LINKS-VIEW opts))
                     (couch/get-view db k/LINKS-VIEW opts))
      routes)))




;; COMMAND
(defn- link-id
  [source-id rel target-id & {:keys [name] :or [name nil]}]
  (if name
    (format "%s--%s>%s-->%s" source-id rel name target-id)
    (format "%s--%s-->%s" source-id rel target-id)))

(defn collaboration-roots
  [doc]
  (get-in doc [:links :_collaboration_roots] []))

(defn- add-roots
  [doc roots]
  (let [current (collaboration-roots doc)]
    (assoc-in doc [:links :_collaboration_roots] (union (set roots) (set current)))))

(defn- update-collaboration-roots-for-target
  "[source target] -> [source target]"
  [source target]

  (let [source-roots (collaboration-roots source)
        source-type (util/entity-type-keyword source)
        target-roots (collaboration-roots target)
        target-type (util/entity-type-keyword target)]
    (cond
      (= source-type :project) [source (add-roots target source-roots)]
      (= target-type :project) [(add-roots source target-roots) target]


      (= source-type :folder) [source (add-roots target source-roots)]
      (= target-type :folder) [(add-roots source target-roots) target]

      ;(and (= source-type :analysisrecord) (= target-type :revision)) [(add-roots source target-roots) target]

      :else
      nil
      )))

(defn- update-collaboration-roots
  "Update source and all target collaboration roots.
  Uses a recursive strategy to update source for each target, while accumulating source and target updates. The only
  tricky bit is that we want a (map) of sources and targets so that we can keep cumulative updates, but also want
  a separate collection of the sources and targets that actually need to be updated. Otherwise, we try to update everything
  even when it's not necessary."
  [source-docs target-docs]

  (loop [sources (util/into-id-map source-docs)
         targets (util/into-id-map target-docs)
         sources-updates {}
         targets-updates {}
         cross (for [source source-docs
                     target target-docs]
                 [(:_id source) (:_id target)])]

    (if-let [[source-id target-id] (first cross)]
      (if-let [[source-update target-update] (update-collaboration-roots-for-target (get sources source-id) (get targets target-id))]
        (recur
          (assoc sources source-id source-update)           ;; sources
          (assoc targets target-id target-update)           ;; targets
          (assoc sources-updates source-id source-update)   ;; sources-updates
          (assoc targets-updates target-id target-update)   ;; targets-updates
          (rest cross))
        (recur
          sources
          targets
          sources-updates
          targets-updates
          (rest cross)))

      {:sources (vals sources-updates)
       :targets (vals targets-updates)})
    ))

(defn make-links
  [authenticated-user-id sources rel targets inverse-rel & {:keys [name]}]

  (for [source sources
        target targets]
    (let [source-roots (collaboration-roots source)
          target-roots (collaboration-roots target)
          source-id (:_id source)
          target-id (:_id target)
          base {:_id       (link-id source-id rel target-id :name name)
                :type      util/RELATION_TYPE
                :target_id (:_id target)
                :source_id (:_id source)
                :rel       rel
                :user_id     authenticated-user-id
                :links     {:_collaboration_roots (concat source-roots target-roots)}}
          named (if name (assoc base :name name) base)]
      (if inverse-rel (assoc named :inverse_rel inverse-rel) named))))

(defn add-links
  "Adds link(s) with the given relation name from doc to each specified target ID. `doc` may be a single doc
  or a Sequential collection of source documents. For each source document, links to all targets are built.

  Returns
  ```{  :updates    <updated documents>
        :links      <new LinkInfo documents>}```
   "
  [auth sources rel target-ids routes & {:keys [inverse-rel name strict required-target-types] :or [inverse-rel nil
                                                                                                    name nil
                                                                                                    strict false
                                                                                                    required-target-types nil]}]

  (let [authenticated-user-id (auth/authenticated-user-id auth)
        targets (core/get-entities auth target-ids routes)]

    (when (and strict (not= (count targets) (count (into #{} target-ids))))
      (throw+ {:type ::target-not-found :message "Target(s) not found"}))

    (let [links (make-links authenticated-user-id sources rel targets inverse-rel :name name)
          updates (update-collaboration-roots sources targets)]
      {:links   links
       :updates (concat (:sources updates) (:targets updates))})))


(defn delete-links
  ([auth routes doc user-id rel target-id & {:keys [name] :or [name nil]}]
   (auth/check! user-id :auth/update doc)
   (let [link-id (link-id (:_id doc) rel target-id :name name)]
      (core/delete-values auth [link-id] routes)))
  ([auth routes doc user-id link-id]
   (auth/check! user-id :auth/update doc)
   (core/delete-values auth [link-id] routes)))
