(ns ovation.test.annotations
  (:import (java.util UUID)
           (us.physion.ovation.domain URIs))
  (:use midje.sweet)
  (:require [ovation.annotations :as a]))


(facts "About annotation links"
       (fact "adds annotation links to entity"
             (a/add-annotation-links {:_id   "123"
                                      :links {:foo "bar"}}) => {:_id   "123"
                                                                :links {:foo             "bar"
                                                                        :properties      "/api/v1/entities/123/annotations/properties"
                                                                        :tags            "/api/v1/entities/123/annotations/tags"
                                                                        :notes           "/api/v1/entities/123/annotations/notes"
                                                                        :timeline-events "/api/v1/entities/123/annotations/timeline-events"}}))



(facts "About annotation endpoints"
       (fact "`union-annotations-map` converts full annotation map to annotation document seq"
             (a/union-annotations-map {:type1 {:user1 #{{:key1 ...val1...} {:key1 ...val2...}}
                                               :user2 #{{:key2 ...val3...} {:key2 ...val4...}}}
                                       :type2 {:user1 #{{:key3 ...val5...} {:key3 ...val6...}}
                                               :user2 #{{:key4 ...val7...} {:key4 ...val8...}}}
                                       }) => (just #{{:key1 ...val1...} {:key1 ...val2...}
                                                     {:key2 ...val3...} {:key2 ...val4...}
                                                     {:key3 ...val5...} {:key3 ...val6...}
                                                     {:key4 ...val7...} {:key4 ...val8...}}))
       (fact "`union-annotations-map` converts annotation type map to document seq"
             (a/union-annotations-map {:user1 #{{:key1 ...val1...} {:key1 ...val2...}}
                                       :user2 #{{:key2 ...val3...} {:key2 ...val4...}}}) => (just #{{:key1 ...val1...} {:key1 ...val2...}
                                                                                                    {:key2 ...val3...} {:key2 ...val4...}}))
       )

