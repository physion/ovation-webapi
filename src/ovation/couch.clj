(ns ovation.couch
  (:require [cemerick.url :as url]
            [com.ashafa.clutch :as cl]
            [clojure.core.async :as async :refer [chan >!! go go-loop >! <!! <! close!]]
            [slingshot.slingshot :refer [throw+ try+]]
            [ovation.http :as http]
            [ovation.util :refer [<??]]
            [ovation.constants :as k]
            [ovation.config :as config]
            [ovation.request-context :as rc]
            [org.httpkit.client :as httpkit.client]
            [ring.util.http-predicates :as http-predicates]
            [ovation.util :as util]
            [ring.util.http-response :refer [throw!]]
            [com.climate.newrelic.trace :refer [defn-traced]]
            [clojure.tools.logging :as logging]
            [ring.util.http-predicates :as hp]))


(def design-doc "api")

(def ncpu (.availableProcessors (Runtime/getRuntime)))

(defn db
  "Database URL from authorization info"
  [auth]
  (logging/debug "DEPRECATED call to couch/db")
  (-> (url/url (config/config :cloudant-db-url))
    (assoc :username (config/config :cloudant-username)
           :password (config/config :cloudant-password))))


(defn- sequential!
  [key]
  (if (sequential? key) key [key]))

(defn prefix-keys
  [opts org prefix]
  (cond
    (contains? opts :key) (assoc opts :key (concat [org prefix] (sequential! (:key opts))))
    (contains? opts :keys) (assoc opts :keys (vec (map #(concat [org prefix] (sequential! %)) (:keys opts))))
    :else (-> opts
            (assoc :startkey (concat [org prefix] (sequential! (:startkey opts))))
            (assoc :endkey (concat [org prefix] (sequential! (:endkey opts)))))))

(def VIEW-PARTITION 20)

(defn get-view-batch
  [view prefixed-opts tf]
  (let [ch   (chan)
        body (util/to-json {:queries prefixed-opts})
        url  (util/join-path [(config/config :cloudant-db-url) "_design" design-doc "_view" view])]
    (http/call-http ch :post url {:body       body
                                  :headers    {"Content-Type" "application/json; charset=utf-8"
                                               "Accept"       "application/json"}
                                  :basic-auth [(config/config :cloudant-username) (config/config :cloudant-password)]} hp/ok?
      :log-response? false)
    (try+
      (let [response (<?? ch)
            results  (:results response)]
        (sequence tf results))
      (catch [:type :ring.util.http-response/response] ex
        (throw! (:response ex))))))


(defn get-view
  "Gets the output of a view, passing opts to clutch/get-view. Runs a query for
  each of owner and team ids, prepending to the start and end keys, and taking the aggregate unique results.

  Use {} (empty map) for a JS object. E.g. :startkey [1 2] :endkey [1 2 {}]"
  [ctx db view opts & {:keys [prefix-teams] :or {prefix-teams true}}]

  (let [org (::rc/org ctx)]

    (cl/with-db db
      (if prefix-teams
        ;; [prefix-teams] Run queries in parallel
        (let [roots         (conj (rc/team-ids ctx) (rc/user-id ctx))
              prefixed-opts (map #(prefix-keys opts org %) roots)
              tf (comp
                   (mapcat :rows)
                   (map :doc)
                   (filter #(not (nil? %)))
                   (distinct))]
          (let [partitions     (partition-all VIEW-PARTITION prefixed-opts)
                thread-results (map
                                 (fn [p]
                                   (async/thread (get-view-batch view p tf)))
                                 partitions)]
            (apply concat (map <?? thread-results))))

        ;; [!prefix-teams] Make single call
        (let [tf (if (:include_docs opts)
                   (comp
                     (map :doc)
                     (distinct))
                   (distinct))]
          (into '() tf (cl/get-view design-doc view opts)))))))


(defn-traced all-docs
  "Gets all documents with given document IDs"
  [ctx db ids]
  (let [partitions     (partition-all VIEW-PARTITION ids)
        {auth ::rc/auth} ctx
        thread-results (map
                         (fn [p]
                           (async/thread (get-view ctx db k/ALL-DOCS-VIEW {:keys          p
                                                                            :include_docs true})))
                         partitions)]

    (apply concat (map <?? thread-results))))               ;;TODO we should use alts!! until all results have come back?

(defn-traced merge-updates
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

(defn-traced bulk-docs
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

(defn-traced delete-docs
  "Deletes documents from the database"
  [db docs]
  (bulk-docs db (map (fn [doc] (assoc doc :_deleted true)) docs)))

(defn search
  [db q & {:keys [bookmark limit] :or [bookmark nil
                                       limit nil]}]

  (let [query-params {:q q :bookmark bookmark :limit limit}
        opts         {:query-params (apply dissoc
                                      query-params
                                      (for [[k v] query-params :when (nil? v)] k))
                      :headers      {"Accept" "application/json"}
                      :basic-auth   [(config/config :cloudant-username) (config/config :cloudant-password)]}
        uri          (util/join-path [db "_design" "search" "_search" "all"])
        resp         @(httpkit.client/get uri opts)]

    (let [body   (-> resp :body util/from-json)
          status (:status resp)]
      (if (http-predicates/ok? resp)
        body
        (let [err (-> body
                    (assoc :status status)
                    (assoc :detail (:reason body))
                    (assoc :title (:error body)))]
          (throw! {:status status
                   :body   {:errors [err]}}))))))
