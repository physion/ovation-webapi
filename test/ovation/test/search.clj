(ns ovation.test.search
  (:require [midje.sweet :refer :all]
            [ovation.search :as search]
            [ovation.core :as core]
            [ovation.routes :as routes]
            [clojure.data.json :as json]
            [qbits.spandex :as spandex]
            [ovation.util :as util]
            [clojure.core.async :as async]))

(against-background [..ctx.. =contains=> {:ovation.request-context/routes ..rt..
                                          :ovation.request-context/org    1}]
  (facts "About search"
    (fact "transforms ES result"
      (let [org            1
            query-response {:took      0,
                            :timed_out false,
                            :_shards   {:total 1, :successful 1, :failed 0},
                            :hits      {:total     ..total_rows..,
                                        :max_score 3.1533415,
                                        :hits      [{:_index  (search/org-index org),
                                                     :_type   "file",
                                                     :_id     ..id..,
                                                     :_score  3.1533415,
                                                     :_source {:attributes  {:name       "PrivateData.csv",
                                                                             :created-at "2017-09-28T03:04:33.286Z",
                                                                             :updated-at "2017-09-28T03:04:48.398Z"},
                                                               :owner       "15cab930-1e24-0131-026c-22000a977b96",
                                                               :projects    [..project_id..],
                                                               :annotations [{:user            "15cab930-1e24-0131-026c-22000a977b96",
                                                                              :annotation      {:tag "findme"},
                                                                              :annotation_type "tags"}]}
                                                     :sort    ["some-sort"]}]}}]

        (search/transform-results ..ctx.. ..db.. query-response) => {:meta           {:total_rows ..total_rows..
                                                                                      :bookmark   (util/b64encode (json/write-str ["some-sort"]))}
                                                                     :search_results [{:id            ..id..
                                                                                       :entity_type   "File"
                                                                                       :name          "PrivateData.csv"
                                                                                       :owner         "15cab930-1e24-0131-026c-22000a977b96"
                                                                                       :updated-at    "2017-09-28T03:04:48.398Z"
                                                                                       :organization  org
                                                                                       :project_names [..project_name..]
                                                                                       :links         {:breadcrumbs ..breadcrumbs..}}]}
        (provided
          (search/breadcrumbs-url ..ctx.. ..id..) => ..breadcrumbs..
          (core/get-entity ..ctx.. ..db.. ..project_id..) => {:attributes {:name ..project_name..}})))


    (fact "runs query"
      (let [query                "some-query"
            org                  1
            query-response       {:took      0,
                                  :timed_out false,
                                  :_shards   {:total 1, :successful 1, :failed 0},
                                  :hits      {:total     2,
                                              :max_score 3.1533415,
                                              :hits      [{:_index  (search/org-index org),
                                                           :_type   "file",
                                                           :_id     ..result_id_1..,
                                                           :_score  3.1533415,
                                                           :_source {:attributes  {:name       "PrivateData.csv",
                                                                                   :created-at "2017-09-28T03:04:33.286Z",
                                                                                   :updated-at "2017-09-28T03:04:48.398Z"},
                                                                     :owner       "15cab930-1e24-0131-026c-22000a977b96",
                                                                     :projects    ["d3a695c4-e7d4-4095-9449-8fd29849f6fe"],
                                                                     :annotations [{:user            "15cab930-1e24-0131-026c-22000a977b96",
                                                                                    :annotation      {:tag "findme"},
                                                                                    :annotation_type "tags"}]}}
                                                          {:_index  "org-247-v1",
                                                           :_type   "file",
                                                           :_id     ..result_id_2..,
                                                           :_score  2.1533415,
                                                           :_source {:attributes  {:name       "PrivateData.csv",
                                                                                   :created-at "2017-09-28T03:48:33.745Z",
                                                                                   :updated-at "2017-09-28T03:48:51.051Z"},
                                                                     :owner       "15cab930-1e24-0131-026c-22000a977b96",
                                                                     :projects    ["52d3bf73-7f05-434e-a198-648480e88cb2"],
                                                                     :annotations [{:user            "15cab930-1e24-0131-026c-22000a977b96",
                                                                                    :annotation      {:tag "findme"},
                                                                                    :annotation_type "tags"}]}}]}}
            get-results-response {:meta           {:bookmark   ..bookmark..
                                                   :total_rows ..total..}
                                  :search_results [..result1.. ..result2..]}]
        (search/search ..ctx.. ..db.. ..client.. query) => {:meta           {:bookmark   ..bookmark..
                                                                             :total_rows ..total..}
                                                            :search_results [..result1.. ..result2..]}
        (provided
          (search/transform-results ..ctx.. ..db.. query-response) => get-results-response
          (spandex/request ..client.. {:url    (util/join-path ["" (search/org-index org) "_search"])
                                       :method :post
                                       :body   {:size  search/MIN-SEARCH
                                                :query {:simple_query_string {:query query}}
                                                :sort  [{:_score "desc"}, {:_uid "asc"}]}}) => {:body query-response})))


    (fact "Generates breadcrumbs URL"
      (search/breadcrumbs-url ..ctx.. "ENTITY") => "breadcrumbs/url?id=ENTITY"
      (provided
        (routes/named-route ..ctx.. :get-breadcrumbs {:org 1}) => "breadcrumbs/url"))
    ))

