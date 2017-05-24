(ns ovation.links
  (:require [ovation.util :refer [create-uri]]
            [ovation.couch :as couch]
            [ovation.util :as util]
            [ovation.auth :as auth]
            [ovation.core :as core]
            [slingshot.slingshot :refer [throw+]]
            [clojure.set :refer [union]]
            [ovation.constants :as k]
            [ovation.transform.read :as tr]
            [ovation.request-context :as request-context]
            [com.climate.newrelic.trace :refer [defn-traced]]))


;; QUERY
(defn- eq-doc-label
  [label]
  (fn [doc] (if-let [doc-label (get-in doc [:attributes :label])]
              (= label doc-label)
              false)))

(defn-traced get-links
  [ctx db id rel & {:keys [label name] :or {label nil name nil}}]
  (let [{auth ::request-context/auth} ctx
        opts {:startkey      (if name [id rel name] [id rel])
              :endkey        (if name [id rel name] [id rel])
              :inclusive_end true
              :reduce        false :include_docs true}]
    (tr/values-from-couch
      (couch/get-view ctx db k/LINK-DOCS-VIEW opts)
      ctx)))

(defn-traced get-link-targets
  "Gets the document targets for id--rel->"
  [ctx db id rel & {:keys [label name include-trashed] :or {label           nil
                                                            name            nil
                                                            include-trashed false}}]
  (let [opts {:startkey      (if name [id rel name] [id rel])
              :endkey        (if name [id rel name] [id rel])
              :inclusive_end true
              :reduce        false :include_docs true}
        docs (if label
               (filter (eq-doc-label label) (couch/get-view ctx db k/LINKS-VIEW opts))
               (couch/get-view ctx db k/LINKS-VIEW opts))]
    (-> docs
      (tr/entities-from-couch ctx)
      (core/filter-trashed include-trashed))))




;; COMMAND
(defn- link-id
  [source-id rel target-id & {:keys [name] :or [name nil]}]
  (if name
    (format "%s--%s>%s-->%s" source-id rel name target-id)
    (format "%s--%s-->%s" source-id rel target-id)))

(defn collaboration-roots
  [doc & {:keys [include-self]
          :or   {include-self true}}]
  (let [roots (get-in doc [:links :_collaboration_roots])]
    (if (and include-self
          (or (empty? roots) (nil? roots)))
      [(:_id doc)]
      roots)))

(defn- add-roots
  [doc roots]
  (let [current (collaboration-roots doc :include-self false)]
    (assoc-in doc [:links :_collaboration_roots] (union (set roots) (set current)))))

(defn- update-collaboration-roots-for-target
  "[source target] -> [source target]"
  [source target]

  (let [source-roots (collaboration-roots source)
        source-type  (util/entity-type-keyword source)
        target-roots (collaboration-roots target)
        target-type  (util/entity-type-keyword target)]
    (cond
      (= source-type :project) [source (add-roots target source-roots)]
      (= target-type :project) [(add-roots source target-roots) target]


      (= source-type :folder) [source (add-roots target source-roots)]
      (= target-type :folder) [(add-roots source target-roots) target]

      (= source-type :file) [source (add-roots target source-roots)]
      (= target-type :file) [(add-roots source target-roots) target]

      (= source-type :source) [(add-roots source target-roots) target]
      (= target-type :source) [source (add-roots target source-roots)]

      :else
      nil)))


(defn- update-collaboration-roots
  "Update source and all target collaboration roots.
  Uses a recursive strategy to update source for each target, while accumulating source and target updates. The only
  tricky bit is that we want a (map) of sources and targets so that we can keep cumulative updates, but also want
  a separate collection of the sources and targets that actually need to be updated. Otherwise, we try to update everything
  even when it's not necessary."
  [source-docs target-docs]

  (loop [sources         (util/into-id-map source-docs)
         targets         (util/into-id-map target-docs)
         sources-updates {}
         targets-updates {}
         cross           (for [source source-docs
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
       :targets (vals targets-updates)})))


(defn-traced make-links
  [org-id authenticated-user-id sources rel targets inverse-rel & {:keys [name]}]

  (for [source sources
        target targets]
    (let [source-roots (collaboration-roots source)
          target-roots (collaboration-roots target)
          source-id    (:_id source)
          target-id    (:_id target)
          base         {:_id          (link-id source-id rel target-id :name name)
                        :type         util/RELATION_TYPE
                        :organization org-id
                        :target_id    (:_id target)
                        :source_id    (:_id source)
                        :rel          (clojure.core/name rel)
                        :user_id      authenticated-user-id
                        :links        {:_collaboration_roots (concat source-roots target-roots)}}
          named        (if name (assoc base :name name) base)]
      (if inverse-rel (assoc named :inverse_rel (clojure.core/name inverse-rel)) named))))

(defn add-links
  "Adds link(s) with the given relation name from doc to each specified target ID. `doc` may be a single doc
  or a Sequential collection of source documents. For each source document, links to all targets are built.

  Returns
  ```{  :updates    <updated documents>
        :links      <new LinkInfo documents>}```
   "
  [ctx db sources rel target-ids & {:keys [inverse-rel name strict] :or [inverse-rel nil
                                                                         name nil
                                                                         strict false]}]

  (let [{auth   ::request-context/auth
         org-id ::request-context/org} ctx
        authenticated-user-id (auth/authenticated-user-id auth)
        targets               (core/get-entities ctx db target-ids)]

    (when (and strict (not= (count targets) (count (into #{} target-ids))))
      (throw+ {:type ::target-not-found :message "Target(s) not found"}))

    (let [links   (make-links org-id authenticated-user-id sources rel targets inverse-rel :name name)
          updates (update-collaboration-roots sources targets)]
      {:links   links
       :updates (concat (:sources updates) (:targets updates))})))


(defn-traced delete-links
  ([ctx db doc rel target-id & {:keys [name] :or [name nil]}]
   (auth/check! ctx ::auth/update doc)
   (let [link-id (link-id (:_id doc) rel target-id :name name)]
     (core/delete-values ctx db [link-id])))
  ([ctx db doc link-id]
   (auth/check! ctx ::auth/update doc)
   (core/delete-values ctx db [link-id])))
