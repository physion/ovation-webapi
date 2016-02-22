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


(defn prefix-keys
  [opts prefix]
  (cond
    (contains? opts :key) (assoc opts :key (cons prefix (if (sequential? (:key opts)) (:key opts) [(:key opts)])))
    (contains? opts :keys) (assoc opts :keys (vec (map #(cons prefix (if (sequential? %) % [%])) (:keys opts))))
    :else (-> opts
            (assoc :startkey (cons prefix (:startkey opts)))
            (assoc :endkey (cons prefix (:endkey opts))))))


(defn get-view
  "Gets the output of a view, passing opts to clutch/get-view. Runs a query for
  each of owner and team ids, prepending to the start and end keys taking unique results.

  Use {} (empty map) for a JS object. E.g. :startkey [1 2] :endkey [1 2 {}]"
  [auth db view opts & {:keys [prefix-teams] :or {prefix-teams true}}]

  (cl/with-db db
    (let [docs (chan (* 2 ncpu) (distinct))]

      ;; Run queries, placing all results onto the docs channel
      (if prefix-teams
        (go-loop [roots (conj (auth/authenticated-teams auth) (auth/authenticated-user-id auth))]
          (if (empty? roots)
            (close! docs)
            (if-let [prefix (first roots)]
              (do (async/onto-chan docs (<! (async/thread (cl/get-view design-doc view (prefix-keys opts prefix)))) false)
                  (recur (rest roots))))))
        (async/onto-chan docs (cl/get-view design-doc view opts)))

      ;; Transform docs to a sequence of results and return a seq from the channel
      (let [results (chan (* 2 ncpu))]
        (async/pipeline ncpu results (if (:include_docs opts)
                                     (comp (map :doc) (filter #(not (nil? %))))
                                     (map identity)) docs)
        (<?? (async/into '() results))))))

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
             %
             ))
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
