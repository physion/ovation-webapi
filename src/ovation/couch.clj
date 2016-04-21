(ns ovation.couch
  (:require [cemerick.url :as url]
            [com.ashafa.clutch :as cl]
            [clojure.core.async :as async :refer [chan >!! go go-loop >! <!! <! close!]]
            [slingshot.slingshot :refer [throw+]]
            [ovation.auth :as auth]
            [ovation.util :refer [<??]]
            [ovation.constants :as k]
            [ovation.config :as config]))

(def design-doc "api")

(def ncpu (.availableProcessors (Runtime/getRuntime)))

(defn db
  "Database URL from authorization info"
  [auth]
  (-> (url/url (config/config "CLOUDANT_DB_URL"))
    (assoc :username (config/config "CLOUDANT_USERNAME")
           :password (config/config "CLOUDANT_PASSWORD"))))


(defn- key-seq
  [key]
  (if (sequential? key) key [key]))

(defn prefix-keys
  [opts prefix]
  (cond
    (contains? opts :key) (assoc opts :key (cons prefix (key-seq (:key opts))))
    (contains? opts :keys) (assoc opts :keys (vec (map #(cons prefix (key-seq %)) (:keys opts))))
    :else (-> opts
            (assoc :startkey (cons prefix (key-seq (:startkey opts))))
            (assoc :endkey (cons prefix (key-seq (:endkey opts)))))))


(defn get-view
  "Gets the output of a view, passing opts to clutch/get-view. Runs a query for
  each of owner and team ids, prepending to the start and end keys taking unique results.

  Use {} (empty map) for a JS object. E.g. :startkey [1 2] :endkey [1 2 {}]"
  [auth db view opts & {:keys [prefix-teams] :or {prefix-teams true}}]

  (cl/with-db db
    (let [docs (chan 1 (if (:include_docs opts)
                         (comp
                           (map :doc)
                           (distinct))
                         (distinct)))]

      ;; Run queries, placing all results onto the docs channel
      (if prefix-teams
        (let [results (loop [roots           (conj (auth/authenticated-teams auth) (auth/authenticated-user-id auth))
                             result-channels nil]
                        (if-let [prefix (first roots)]
                          (let [c (chan)]
                            (async/thread
                              (let [r (cl/get-view design-doc view (prefix-keys opts prefix))]
                                (async/onto-chan c r)))
                            (recur (rest roots) (conj result-channels c)))
                          (async/merge result-channels)))]
          (async/pipe results docs))

        ;; Make single call, placing results onto docs channel
        (async/onto-chan docs (cl/get-view design-doc view opts) true))

      ;; Pull docs into a list
      (<?? (async/into '() docs)))))

(defn all-docs
  "Gets all documents with given document IDs"
  [auth db ids]
  (get-view auth db k/ALL-DOCS-VIEW {:keys         ids
                                     :include_docs true}))

(defn merge-updates
  "Merges _rev updates (e.g. via bulk-update) into the documents in docs."
  [docs updates]
  (let [update-map (into {} (map (fn [doc] [(:id doc) (:rev doc)]) updates))]
    (map #(if-let [rev (update-map (:_id %))]
           (assoc % :_rev rev)
           (case (:error %)
             "conflict" (throw+ {:type ::conflict :message "Document conflict" :id (:_id %)})
             "forbidden" (throw+ {:type ::forbidden :message "Update forbidden" :id (:_id %)})
             "unauthorized" (throw+ {:type ::unauthorized :message "Update unauthorized" :id (:id %)})
             %))

      docs)))

(defn bulk-docs
  "Creates or updates documents"
  [db docs]
  (cl/with-db db
    (merge-updates docs (cl/bulk-update docs))))

(defn changes
  "Writes changes to channel c.

  Options:
    :continuous [true|false]
    :since <db-seq>"
  [db c & {:as opts}]
  (let [changes-agent (cl/change-agent db opts)]
    (add-watch changes-agent :update (fn [key ref old new] (go (>! c new))))
    (cl/start-changes changes-agent)))

(defn delete-docs
  "Deletes documents from the database"
  [db docs]
  (bulk-docs db (map (fn [doc] (assoc doc :_deleted true)) docs)))
