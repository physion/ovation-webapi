(ns ovation.search
  (:require [ovation.core :as core]
            [ovation.request-context :as request-context]
            [ovation.routes :as routes]
            [ovation.links :as links]
            [ovation.util :as util]
            [qbits.spandex :as spandex]
            [clojure.data.json :as json]
            [clojure.core.async :as async :refer [<! <!! chan]]
            [clojure.string :as string]
            [clojure.tools.logging :as logging]))

(defn breadcrumbs-url
  [ctx id]
  (str (routes/named-route ctx :get-breadcrumbs {:org (::request-context/org ctx)}) "?id=" id))

(defn transform-results
  [ctx db elastic-result]
  (let [meta    {:total_rows (get-in elastic-result [:hits :total] 0)
                 :bookmark   (util/b64encode (json/write-str (or (:sort (last (get-in elastic-result [:hits :hits]))) [])))}
        results (map (fn [doc]
                       {:id            (:_id doc)
                        :entity_type   (string/capitalize (:_type doc))
                        :name          (get-in doc [:_source :attributes :name] (:_id doc))
                        :owner         (get-in doc [:_source :owner])
                        :updated-at    (get-in doc [:_source :attributes :updated-at])
                        :organization  (::request-context/org ctx)
                        :project_names (if-let [project-ids (get-in doc [:_source :projects])]
                                         (remove nil? (map (fn [project-id]
                                                             (if-let [project (core/get-entity ctx db project-id)]
                                                               (get-in project [:attributes :name])
                                                               nil)) project-ids))
                                         [])
                        :links         {:breadcrumbs (breadcrumbs-url ctx (:_id doc))}}) (get-in elastic-result [:hits :hits]))]
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
        query-base       {:size  (max MIN-SEARCH limit)
                          :query {:simple_query_string {:query q}}
                          :sort  [{:_score "desc"}, {:_id "asc"}]}
        query            (if (nil? bookmark) query-base (assoc query-base :search_after (json/read-str (util/b64decode bookmark))))
        es-response      (spandex/request client {:url    (util/join-path ["" (org-index org) "_search"])
                                                  :method :post
                                                  :body   query})
        es-response-body (:body es-response)]

    (transform-results ctx db es-response-body)))
