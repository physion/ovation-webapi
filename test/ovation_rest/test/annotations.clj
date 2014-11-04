(ns ovation-rest.test.annotations
  (:import (java.util UUID)
           (us.physion.ovation.domain URIs))
  (:use midje.sweet)
  (:require [ovation-rest.annotations :as a]))


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
                                      :user2 #{...doc3... ...doc4...}}
                              :type2 {:user1 #{...doc5... ...doc6...}
                                      :user2 #{...doc7... ...doc8...}}}) => (just #{{:key1 ...val1...} {:key1 ...val2...} ...doc3... ...doc4... ...doc5... ...doc6... ...doc7... ...doc8...}))
  (fact "`union-annotations-map` converts annotation type map to document seq"
    (a/union-annotations-map {:user1 #{{:key1 ...val1...} {:key1 ...val2...}}
                              :user2 #{...doc3... ...doc4...}}) => (just #{{:key1 ...val1...} {:key1 ...val2...} ...doc3... ...doc4... })))

