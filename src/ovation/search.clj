(ns ovation.search
  (:require [ovation.core :as core]
            [ovation.request-context :as request-context]
            [ovation.routes :as routes]
            [ovation.links :as links]
            [ovation.util :as util]
            [qbits.spandex :as spandex]
            [clojure.core.async :as async :refer [<! <!! chan]]))

(defn breadcrumbs-url
  [ctx id]
  (str (routes/named-route ctx :get-breadcrumbs {:org (::request-context/org ctx)}) "?id=" id))

(defn transform-results
  [ctx db elastic-result]
  (let [meta    {:total_rows (get-in elastic-result [:hits :total])
                 :bookmark   (:sort elastic-result)}
        results (map (fn [doc]
                       (let [entity (core/get-entity ctx db (:_id doc))]
                         {:id            (:_id entity)
                          :entity_type   (:type entity)
                          :name          (get-in entity [:attributes :name] (:_id entity))
                          :owner         (:owner entity)
                          :updated-at    (get-in entity [:attributes :updated-at])
                          :organization  (::request-context/org ctx)
                          :project_names (if-let [project-ids (get-in doc [:_source :projects])]
                                           (remove nil? (map (fn [project-id]
                                                               (if-let [project (core/get-entity ctx db project-id)]
                                                                 (get-in project [:attributes :name])
                                                                 nil)) project-ids))
                                           [])
                          :links         {:breadcrumbs (breadcrumbs-url ctx (:_id entity))}})) (get-in elastic-result [:hits :hits]))]
    {:meta    meta
     :search_results results}))

(defn org-index
  [org]
  (format "org-%d-v1" org))

(def MIN-SEARCH 25)
(defn search
  [ctx db client q & {:keys [bookmark limit] :or {bookmark nil
                                                  limit    0}}]
  (let [org              (::request-context/org ctx)
        query-base       {:query {:size                (max MIN-SEARCH limit)
                                  :simple_query_string {:query  q}
                                  :sort                [{:_score "desc"}, {:_uid "asc"}]}}
        query            (if (nil? bookmark) query-base (assoc-in query-base [:query :search_after] bookmark))
        elastic-response (spandex/request client {:url    (util/join-path [(org-index org) "_search"])
                                                  :method :post
                                                  :body   query})]

    (transform-results ctx db elastic-response)))
