(ns ovation.test.search
  (:require [midje.sweet :refer :all]
            [ovation.search :as search]
            [ovation.couch :as couch]))


(facts "About search"
  (fact "calls Cloudant search"
    (search/search ..auth.. ..rt.. ..q..) => {:data [..result1.. ..result2..]
                                              :metadata {:bookmark ..bookmark..
                                                         :total_rows ..total..}}
    (provided
      (couch/db ..auth..) => ..db..
      (couch/search ..db.. ..q..) => {:total_rows ..total..
                                      :bookmark ..bookmark..
                                      :rows [{:id ..id1..
                                              :order [3.9 107]
                                              :fields {:id ..id1..
                                                       :type ..type1..}}
                                             {:id ..id2..
                                              :order [3.9 107]
                                              :fields {:id ..id2..
                                                       :type ..type2..}}]})))
