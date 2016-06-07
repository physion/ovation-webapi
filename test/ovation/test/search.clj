(ns ovation.test.search
  (:require [midje.sweet :refer :all]
            [ovation.search :as search]
            [ovation.couch :as couch]
            [ovation.constants :as k]
            [ovation.core :as core]))


(facts "About search"
  (fact "calls Cloudant search"
    (search/search ..auth.. ..rt.. ..q..) => {:data [..result1.. ..result2..]
                                              :metadata {:bookmark ..bookmark..
                                                         :total_rows ..total..}}
    (provided
      (couch/db ..auth..) => ..db..
      (core/get-entities ..auth.. (seq [..id1.. ..id2..]) ..rt.) => [..result1.. ..result2..]
      (couch/search ..db.. ..q..) => {:total_rows ..total..
                                      :bookmark ..bookmark..
                                      :rows [{:id ..id1..
                                              :order [3.9 107]
                                              :fields {:id ..id1..
                                                       :type k/PROJECT-TYPE}}
                                             {:id ..id2..
                                              :order [3.9 107]
                                              :fields {:id ..id2..
                                                       :type k/REVISION-TYPE}}]}))

  (fact "Returns entity for Annotation"
    (search/search ..auth.. ..rt.. ..q..) => {:data [..entity..]
                                              :metadata {:bookmark ..bookmark..
                                                         :total_rows ..total..}}
    (provided
      (couch/db ..auth..) => ..db..
      (core/get-entities ..auth.. [..entity..] ..rt..) => [..entity..]
      (couch/search ..db.. ..q..) => {:total_rows ..total..
                                      :bookmark   ..bookmark..
                                      :rows       [{:id     ..id1..
                                                    :order  [3.9 107]
                                                    :fields {:id   ..id1..
                                                             :entity ..entity..
                                                             :type k/ANNOTATION-TYPE}}]})))

