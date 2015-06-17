(ns ovation.test.annotations
  (:use midje.sweet)
  (:require [ovation.annotations :as a]
            [ovation.util :as util]
            [ovation.couch :as couch]
            [ovation.auth :as auth]
            [ovation.core :as core]))


(against-background [(couch/db ..auth..) => ..db..]
  (facts "About `get-annotations`"
    (fact "returns annotation documents grouped by entity and user"
      (let [id1 (str (util/make-uuid))
            id2 (str (util/make-uuid))
            user1 (str (util/make-uuid))
            user2 (str (util/make-uuid))
            a1 {:type            "Annotation"
                :annotation_type ..type..
                :_rev            ..rev..
                :entity          id1
                :user            user1}
            a2 {:type            "Annotation"
                :annotation_type ..type..
                :_rev            ..rev..
                :entity          id1
                :user            user2}
            a3 {:type            "Annotation"
                :annotation_type ..type..
                :_rev            ..rev..
                :entity          id2
                :user            user1}
            a4 {:type            "Annotation"
                :annotation_type ..type..
                :_rev            ..rev..
                :entity          id2
                :user            user2}]
        (a/get-annotations ..auth.. [id1 id2] ..type..) => {(keyword id1) {(keyword user1) [a1]
                                                                           (keyword user2) [a2]}
                                                            (keyword id2) {(keyword user1) [a3]
                                                                           (keyword user2) [a4]}}
        (provided
          (couch/get-view ..db.. "annotation_docs" {:keys         [[id1 ..type..]
                                                                   [id2 ..type..]]
                                                    :include_docs true
                                                    :reduce       false}) => (seq [a1 a2 a3 a4])))))

  (facts "About `create-annotations`"
    (fact "creates annotation documents"
      (let [expected [{:entity          ..id1..
                       :user            ..user..
                       :annotation_type ..type..
                       :type            "Annotation"
                       :annotation      {:tag ..tag..}
                       :links           {:_collaboration_roots [..root1..]}}

                      {:entity          ..id2..
                       :user            ..user..
                       :annotation_type ..type..
                       :type            "Annotation"
                       :annotation      {:tag ..tag..}
                       :links           {:_collaboration_roots [..root2..]}}
                      ]]
        (a/create-annotations ..auth.. [..id1.. ..id2..] ..type.. [{:tag ..tag..}]) => ..result..
        (provided
          (auth/authenticated-user-id ..auth..) => ..user..
          (core/get-entities ..auth.. [..id1.. ..id2..]) => [{:_id   ..id1..
                                                              :links {:_collaboration_roots [..root1..]}}
                                                             {:_id   ..id2..
                                                              :links {:_collaboration_roots [..root2..]}}]
          (core/create-values ..auth.. expected) => ..result..))))

  (facts "About `delete-annotations`"
    (fact "calls `delete-entities"
      (a/delete-annotations ..auth.. [..id1.. ..id2..]) => ..result..
      (provided
        (core/delete-values ..auth.. [..id1.. ..id2..]) => ..result..))))

