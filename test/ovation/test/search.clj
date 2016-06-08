(ns ovation.test.search
  (:require [midje.sweet :refer :all]
            [ovation.search :as search]
            [ovation.couch :as couch]
            [ovation.constants :as k]
            [ovation.core :as core]
            [ovation.routes :as routes]
            [ovation.links :as links]))


(facts "About search"
  (fact "transforms Cloudant search"
    (search/search ..auth.. ..rt.. ..q..) => {:search_results [..result1.. ..result2..]
                                              :metadata       {:bookmark ..bookmark..
                                                               :total_rows ..total..}}
    (provided
      (couch/db ..auth..) => ..db..
      (search/get-results ..auth.. ..rt.. [{:id     ..id1..
                                            :order  [3.9 107]
                                            :fields {:id   ..id1..
                                                     :type k/PROJECT-TYPE}}
                                           {:id     ..id2..
                                            :order  [3.9 107]
                                            :fields {:id   ..id2..
                                                     :type k/REVISION-TYPE}}]) => [..result1.. ..result2..]
      (couch/search ..db.. ..q.. :bookmark nil) => {:total_rows ..total..
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
      (search/get-results ..auth.. ..rt.. rows) => [{:id ..eid.. :entity_type k/PROJECT-TYPE :name ..project.. :project_names [..project..] :links {:breadcrumbs ..bc1..}}
                                                    {:id ..id1.. :entity_type k/FILE-TYPE :name ..file.. :project_names [..fileproject..] :links {:breadcrumbs ..bc2..}}]
      (provided
        (search/breadcrumbs-url ..rt.. ..eid..) => ..bc1..
        (search/breadcrumbs-url ..rt.. ..id1..) => ..bc2..
        (search/entity-ids rows) => [..eid.. ..id1..]
        (links/collaboration-roots {:_id ..eid..
                                    :type k/PROJECT-TYPE
                                    :attributes {:name ..project..}}) => [..eid..]
        (links/collaboration-roots {:_id ..id1..
                                    :type k/FILE-TYPE
                                    :attributes {:name ..file..}}) => [..fileprojectid..]
        (core/get-entities ..auth.. [..eid.. ..fileprojectid..] ..rt..) => [{:_id ..fileprojectid..
                                                                             :type k/PROJECT-TYPE
                                                                             :attributes {:name ..fileproject..}}
                                                                            {:_id ..eid..
                                                                             :type k/PROJECT-TYPE
                                                                             :attributes {:name ..project..}}]
        (core/get-entities ..auth.. [..eid.. ..id1..] ..rt..) => [{:_id ..eid..
                                                                   :type k/PROJECT-TYPE
                                                                   :attributes {:name ..project..}}
                                                                  {:_id ..id1..
                                                                   :type k/FILE-TYPE
                                                                   :attributes {:name ..file..}}])))

  (fact "Generates breadcrumbs URL"
    (search/breadcrumbs-url ..rt.. "ENTITY") => "breadcrumbs/url?id=ENTITY"
    (provided
      (routes/named-route ..rt.. :get-breadcrumbs {}) => "breadcrumbs/url"))

  (fact "Gets entity ID from annotations"
    (search/entity-ids [{:id     ..id1..
                         :order  [3.9 107]
                         :fields {:id   ..id1..
                                  :type k/PROJECT-TYPE}}
                        {:id     ..id2..
                         :order  [3.9 107]
                         :fields {:id     ..eid..
                                  :type   k/ANNOTATION-TYPE}}]) => [..id1.. ..eid..]))

