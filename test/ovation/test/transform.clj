(ns ovation.test.transform
  (:use midje.sweet)
  (:require [ovation.transform :as t]))


(facts "About annotation links"
  (fact "adds annotation links to entity"
    (t/add-annotation-links {:_id   "123"
                             :links {:foo "bar"}}) => {:_id   "123"
                                                       :links {:foo             "bar"
                                                               :properties      "/api/v1/entities/123/annotations/properties"
                                                               :tags            "/api/v1/entities/123/annotations/tags"
                                                               :notes           "/api/v1/entities/123/annotations/notes"
                                                               :timeline-events "/api/v1/entities/123/annotations/timeline-events"}}))

(facts "About DTO link modifications"
  (fact "`remove-hidden-links` removes '_...' links"
    (t/remove-private-links {:_id   ...id...
                             :links {"_collaboration_links" #{...hidden...}
                                     :link1                 ...link1...}}) => {:_id   ...id...
                                                                               :links {:link1 ...link1...}})
  (fact "`add-self-link` adds a self link to a DTO"
    (t/add-self-link ...link... {:links {:foo ...foo...}}) => {:links {:foo  ...foo...
                                                                       :self ...link...}}))

(facts "About doc-to-couch"
  (fact "adds collaboration roots"
    (let [doc {:type ..type.. :attributes {:label ..label..}}]
      (t/doc-to-couch ..owner.. ..roots.. doc) =contains=> (assoc-in doc [:links :_collaboration_roots] ..roots..)))

  (fact "adds owner element"
    (let [doc {:type ..type.. :attributes {:label ..label..}}]
      (t/add-owner doc ..owner..) => (assoc doc :owner ..owner..))))

