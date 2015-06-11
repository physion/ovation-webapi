(ns ovation.test.transform
  (:use midje.sweet)
  (:require [ovation.transform.read :as tr]
            [ovation.transform.write :as tw]
            [ovation.version :refer [version]]
            [ovation.util :as util])
  (:import (java.util UUID)))


(facts "About annotation links"
  (fact "adds annotation links to entity"
    (tr/add-annotation-links {:_id   "123"
                              :links {:foo "bar"}}) => {:_id   "123"
                                                        :links {:foo             "bar"
                                                                :properties      "/api/v1/entities/123/annotations/properties"
                                                                :tags            "/api/v1/entities/123/annotations/tags"
                                                                :notes           "/api/v1/entities/123/annotations/notes"
                                                                :timeline-events "/api/v1/entities/123/annotations/timeline-events"}}))

(facts "About DTO link modifications"
  (fact "`remove-hidden-links` removes '_...' links"
    (tr/remove-private-links {:_id   ...id...
                              :links {"_collaboration_links" #{...hidden...}
                                      :link1                 ...link1...}}) => {:_id   ...id...
                                                                                :links {:link1 ...link1...}})

  (fact "`links-to-rel-path` updates links to API relative path"
    (let [couch {:_id   ..id..
                 :links {:link1 "ovation://blahblah"
                         :link2 "ovation://blablah/blah"}}]
      (tr/links-to-rel-path couch) => {:_id         ..id..
                                       :named_links {}
                                       :links       {:link1 (util/join-path ["/api" version "entities" ..id.. "links" "link1"])
                                                     :link2 (util/join-path ["/api" version "entities" ..id.. "links" "link2"])}}))
  (fact "`add-self-link` adds self link"
    (let [couch {:_id   ..id..
                 :links {}}]
      (tr/add-self-link couch) => {:_id   ..id..
                                   :links {:self (util/join-path ["/api" version "entities" ..id..])}}))

  (fact "`links-to-rel-path` updates named links to relative path"
    (let [couch {:_id         ..id..
                 :named_links {:link1 {:name1 "ovation://blahblah"
                                       :name2 "ovation://blablah/blah"}}}
          ]
      (tr/links-to-rel-path couch) => {:_id         ..id..
                                       :links       {}
                                       :named_links {:link1 {:name1 (str (util/join-path ["/api" version "entities" ..id.. "links" "link1"]) "?name=name1")
                                                             :name2 (str (util/join-path ["/api" version "entities" ..id.. "links" "link1"]) "?name=name2")}}})))

(facts "About doc-to-couch"
  (fact "adds collaboration roots"
    (let [doc {:type ..type.. :attributes {:label ..label..}}]
      (tw/doc-to-couch ..owner.. ..roots.. doc) =contains=> (assoc-in doc [:links :_collaboration_roots] ..roots..))))


(facts "About `add-owner`"
  (fact "`add-owner` adds owner element"
    (let [doc {:type ..type.. :attributes {:label ..label..}}]
      (tw/add-owner doc ..owner..) => (assoc doc :owner ..owner..))))

(facts "About `remove-user-attributes`"
  (fact "Removes User entity attributes"
    (let [user {:type "User" :attributes {:password ..secret..}}]
      (tr/remove-user-attributes user) => (assoc user :attributes {})))

  (fact "Retains User :name attribute"
    (let [user {:type "User" :attributes {:password ..secret.. :name ..name..}}]
      (tr/remove-user-attributes user) => (assoc user :attributes {:name ..name..})))

  (fact "Does not remove other entity attributes"
    (let [doc {:type "MyEntity" :attributes {:label ..label..}}]
      (tr/remove-user-attributes doc) => doc)))

(facts "About `couch-to-doc`"
  (fact "calls `remove-user-attributes`"
    (let [doc {:type "Doc" :attributes {:foo "bar"}}]

      (tr/couch-to-doc doc) => (contains doc)
      (provided
        (tr/remove-user-attributes doc) => doc))))

