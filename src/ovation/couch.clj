(ns ovation.couch
  (:require [cemerick.url :as url]
            [com.ashafa.clutch :as cl]
            [clojure.core.async :as async :refer [chan >!! go go-loop >! <!! <! close!]]
            [slingshot.slingshot :refer [throw+]]
            [ovation.auth :as auth]
            [ovation.util :refer [<??]]))

(def design-doc "api")

(def ncpu (.availableProcessors (Runtime/getRuntime)))

(defn db
  "Database URL from authorization info"
  [auth]
  (-> (url/url (:cloudant_db_url auth))
    (assoc :username (:cloudant_key auth)
           :password (:cloudant_password auth))))

(defn get-view
  "Gets the output of a view, passing opts to clutch/get-view. Runs a query for
  each of owner and team ids, prepending to the start and end keys taking unique results.

  Use {} (empty map) for a JS object. E.g. :startkey [1 2] :endkey [1 2 {}]"
  [auth db view opts & {:keys [prefix-teams] :or {prefix-teams true}}]

  (cl/with-db db
    (let [docs (chan ncpu (distinct))]

      ;; Run queries
      (if prefix-teams
        (go-loop [roots (conj (auth/teams auth) (auth/authenticated-user-id auth))]
          (if (empty? roots)
            (close! docs)
            (if-let [prefix (first roots)]
              (let [prefixed-ops (-> opts
                                   (assoc :startkey (cons prefix (:startkey opts)))
                                   (assoc :endkey (cons prefix (:endkey opts))))]
                (async/onto-chan docs (<! (async/thread (cl/get-view design-doc view prefixed-ops))) false)
                (recur (rest roots))))))
        (async/onto-chan docs (cl/get-view design-doc view opts)))

      ;; Transform to a sequence of results
      (let [results (chan 10)]
        (async/pipeline 10 results (if (:include_docs opts)
                                     (comp (map :doc) (filter #(not (nil? %))))
                                     (map identity)) docs)
        (<?? (async/into '() results))))))

(defn all-docs
  "Gets all documents with given document IDs"
  [auth db ids]
  (cl/with-db db
    (map :doc (cl/all-documents {:reduce false :include_docs true} {:keys ids}))))

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
