(ns ovation.links
  (:require [ovation.util :refer [create-uri]]
            [ovation.couch :as couch]
            [ovation.util :as util]
            [ovation.auth :as auth]
            [ovation.core :as core]
            [slingshot.slingshot :refer [throw+]]
            [clojure.set :refer [union]])
  (:import (us.physion.ovation.data EntityDao$Views)))



;; QUERY
(defn- eq-doc-label
  [label]
  (fn [doc] (if-let [doc-label (get-in doc [:attributes :label])]
              (= label doc-label)
              false)))

(defn get-link-targets
  "Gets the document targets for id--rel->"
  [auth id rel & {:keys [label name] :or {label nil name nil}}]
  (let [db (couch/db auth)
        opts {:key    (if name [id rel name] [id rel])
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
    (assoc-in doc [:links :_collaboration_roots] (union (set roots) (set current)))))

(defn- update-collaboration-roots-for-target
  "[source target] -> [source target]"
  [source target]

  (if target
    (let [source-roots (collaboration-roots source)
          source-type (clojure.string/lower-case (:type source))
          target-roots (collaboration-roots target)
          target-type (clojure.string/lower-case (:type target))]
      (cond
        (= source-type "project") [source (add-roots target source-roots)]
        (= target-type "project") [(add-roots source target-roots) target]


        (= source-type "folder") [source (add-roots target source-roots)]
        (= target-type "folder") [(add-roots source target-roots) target]

        (= source-type "experiment") [source (add-roots target source-roots)]
        (= target-type "experiment") [(add-roots source target-roots) target]

        (= target-type "source") [source (add-roots target source-roots)]

        (= target-type "protocol") [source (add-roots target source-roots)]

        :else
        [source target]
        ))
    [source target]))

(defn- update-collaboration-roots
  "Update source and all target collaboration roots.
  Uses a recursive strategy to update source for each target, while accumulating target updates"
  [source targets]

  (let [[updated-src updated-targets] (loop [src source
                                             targets targets
                                             updated-targets '()]
                                        (let [target (first targets)]
                                          (if (nil? target)
                                            [src updated-targets]
                                            (let [[updated-src updated-target] (update-collaboration-roots-for-target src (first targets))]
                                              (recur updated-src (rest targets) (conj updated-targets updated-target))))))]

    (conj updated-targets updated-src)))



(defn add-links
  "Adds link(s) with the given relation name from doc to each specified target ID. `doc` may be a single doc
  or a Sequential collection of source documents. For each source document, links to all targets are built.

  Returns
  ```{:updates <updated documents>
   :links <new link documents
   :all (concat :updates :links)}```
   "
  [auth doc rel target-ids & {:keys [inverse-rel name strict required-target-types] :or [inverse-rel nil
                                                                                         name nil
                                                                                         strict false
                                                                                         required-target-types nil]}]

  (let [authenticated-user-id (auth/authenticated-user-id auth)
        unique-targets (into #{} target-ids)]

    (loop [docs (if (sequential? doc) doc [doc])
           updates-acc (util/into-id-map docs)
           links-acc '()]
      (let [doc (first docs)]
        (if (empty? docs)
          (let [updates (vals updates-acc)
                links links-acc]
            {:updates updates
             :links   links
             :all     (concat updates links)})
          (do
            (auth/check! authenticated-user-id :auth/update doc)
            (let [doc-id (:_id doc)
                  path (link-path doc-id rel name)
                  linked-doc (if name
                               (assoc-in doc [:named_links (keyword rel) (keyword name)] path)
                               (assoc-in doc [:links (keyword rel)] path))
                  targets (core/get-entities auth unique-targets)]

              (if (and strict
                    (not (= (count targets) (count unique-targets))))

                (throw+ {:type ::target-not-found :message "Target(s) not found"})

                (let [target-types (map :type targets)]
                  (if (or (nil? required-target-types)
                        (every? (into #{} required-target-types) target-types))
                    (let [links (map (fn [target-id]
                                       (let [base {:_id       (link-id doc-id rel target-id :name name)
                                                   :target_id target-id
                                                   :source_id doc-id
                                                   :rel       rel
                                                   :user_id   authenticated-user-id}
                                             named (if name (assoc base :name name) base)
                                             link (if inverse-rel (assoc named :inverse_rel inverse-rel) named)]
                                         link))
                                  unique-targets)
                          updated-docs (util/into-id-map (update-collaboration-roots linked-doc targets))]

                      (recur (rest docs) (merge updates-acc updated-docs) (concat links-acc links)))
                    (throw+ {:type ::illegal-target-type :message "Target(s) not of required type(s)"})))))))))))

(defn delete-link
  [auth doc user-id rel target-id & {:keys [inverse-rel name] :or [inverse-rel nil
                                                                   name nil]}]
  (auth/check! user-id :auth/update doc)
  (let [link-id (link-id (:_id doc) rel target-id :name name)
        db (couch/db auth)
        links (couch/all-docs db [link-id])]
    (couch/delete-docs db links)))
