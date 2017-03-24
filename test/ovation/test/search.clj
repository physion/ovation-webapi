(ns ovation.test.search
  (:require [midje.sweet :refer :all]
            [ovation.search :as search]
            [ovation.couch :as couch]
            [ovation.constants :as k]
            [ovation.core :as core]
            [ovation.routes :as routes]
            [ovation.links :as links]))


(against-background [..ctx.. =contains=> {:ovation.request-context/routes ..rt..}]
  (facts "About search"
    (fact "transforms Cloudant search"
      (search/search ..ctx.. ..db.. ..q..) => {:search_results [..result1.. ..result2..]
                                               :meta           {:bookmark   ..bookmark..
                                                                :total_rows ..total..}}
      (provided
        (search/get-results ..ctx.. ..db.. [{:id     ..id1..
                                             :order  [3.9 107]
                                             :fields {:id   ..id1..
                                                      :type k/PROJECT-TYPE}}
                                            {:id     ..id2..
                                             :order  [3.9 107]
                                             :fields {:id   ..id2..
                                                      :type k/REVISION-TYPE}}]) => [..result1.. ..result2..]
        (couch/search ..db.. ..q.. :bookmark nil :limit search/MIN-SEARCH) => {:total_rows ..total..
                                                                               :bookmark   ..bookmark..
                                                                               :rows       [{:id     ..id1..
                                                                                             :order  [3.9 107]
                                                                                             :fields {:id   ..id1..
                                                                                                      :type k/PROJECT-TYPE}}
                                                                                            {:id     ..id2..
                                                                                             :order  [3.9 107]
                                                                                             :fields {:id   ..id2..
                                                                                                      :type k/REVISION-TYPE}}]}))

    (fact "Extracts entity ids"
      (let [rows [{:id     ..id1..
                   :order  [3.9 107]
                   :fields {:id   ..id1..
                            :type k/PROJECT-TYPE}}
                  {:id     ..id2..
                   :order  [3.9 107]
                   :fields {:id     ..id2..
                            :entity ..eid..
                            :type   k/ANNOTATION-TYPE}}]]
        (search/get-results ..ctx.. ..db.. rows) => [{:id            ..eid..
                                                      :entity_type   k/PROJECT-TYPE
                                                      :name          ..project..
                                                      :project_names [..project..]
                                                      :owner         ..user1..
                                                      :updated-at    ..update1..
                                                      :links         {:breadcrumbs ..bc1..}}
                                                     {:id            ..id1..
                                                      :entity_type   k/FILE-TYPE
                                                      :name          ..file..
                                                      :owner         ..user2..
                                                      :updated-at    ..update2..
                                                      :project_names [..fileproject..]
                                                      :links         {:breadcrumbs ..bc2..}}]
        (provided
          (search/breadcrumbs-url ..ctx.. ..eid..) => ..bc1..
          (search/breadcrumbs-url ..ctx.. ..id1..) => ..bc2..
          (search/entity-ids rows) => [..eid.. ..id1..]
          (links/collaboration-roots {:_id        ..eid..
                                      :type       k/PROJECT-TYPE
                                      :owner      ..user1..
                                      :attributes {:name       ..project..
                                                   :updated-at ..update1..}}) => [..eid..]
          (links/collaboration-roots {:_id        ..id1..
                                      :type       k/FILE-TYPE
                                      :owner      ..user2..
                                      :attributes {:name ..file.. :updated-at ..update2..}}) => [..fileprojectid..]
          (core/get-entities ..ctx.. ..db.. [..eid.. ..fileprojectid..]) => [{:_id        ..fileprojectid..
                                                                              :type       k/PROJECT-TYPE
                                                                              :owner      ..user2..
                                                                              :attributes {:name       ..fileproject..
                                                                                           :updated-at ..update2..}}
                                                                             {:_id        ..eid..
                                                                              :type       k/PROJECT-TYPE
                                                                              :owner      ..user1..
                                                                              :attributes {:name       ..project..
                                                                                           :updated-at ..update1..}}]
          (core/get-entities ..ctx.. ..db.. [..eid.. ..id1..]) => [{:_id        ..eid..
                                                                    :type       k/PROJECT-TYPE
                                                                    :owner      ..user1..
                                                                    :attributes {:name       ..project..
                                                                                 :updated-at ..update1..}}
                                                                   {:_id        ..id1..
                                                                    :type       k/FILE-TYPE
                                                                    :owner      ..user2..
                                                                    :attributes {:name       ..file..
                                                                                 :updated-at ..update2..}}])))

    (fact "Generates breadcrumbs URL"
      (search/breadcrumbs-url ..ctx.. "ENTITY") => "breadcrumbs/url?id=ENTITY"
      (provided
        (routes/named-route ..ctx.. :get-breadcrumbs {}) => "breadcrumbs/url"))

    (fact "Gets entity ID from annotations"
      (search/entity-ids [{:id     ..id1..
                           :order  [3.9 107]
                           :fields {:id   ..id1..
                                    :type k/PROJECT-TYPE}}
                          {:id     ..id2..
                           :order  [3.9 107]
                           :fields {:id   ..eid..
                                    :type k/ANNOTATION-TYPE}}]) => [..id1.. ..eid..])))

