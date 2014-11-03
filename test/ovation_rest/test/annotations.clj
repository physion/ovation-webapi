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



