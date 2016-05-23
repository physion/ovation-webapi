(ns ovation.test.annotations
  (:use midje.sweet)
  (:require [ovation.annotations :as a]
            [ovation.util :as util]
            [ovation.couch :as couch]
            [ovation.auth :as auth]
            [ovation.core :as core]
            [ovation.constants :as c]
            [ring.util.http-response :refer [unprocessable-entity forbidden] :as http-response])
  (:import (clojure.lang ExceptionInfo)))


(defn sling-throwable
  [exception-map]
  (slingshot.support/get-throwable (slingshot.support/make-context
                                     exception-map
                                     (str "throw+: " map)
                                     nil
                                     (slingshot.support/stack-trace))))

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
        (a/get-annotations ..auth.. [id1 id2] ..type..) => [a1 a2 a3 a4]
        ;{(keyword id1) {(keyword user1) [a1]
        ;                                                                   (keyword user2) [a2]}
        ;                                                    (keyword id2) {(keyword user1) [a3]
        ;                                                                   (keyword user2) [a4]}}
        (provided
          (couch/get-view ..auth.. ..db.. "annotation_docs" {:keys         [[id1 ..type..]
                                                                            [id2 ..type..]]
                                                             :include_docs true
                                                             :reduce       false}) => (seq [a1 a2 a3 a4])))))

  (facts "About `create-annotations`"
    (fact "creates annotation documents"
      (let [expected [{:_id             ..uuid..
                       :entity          ..id1..
                       :user            ..user..
                       :annotation_type ..type..
                       :type            "Annotation"
                       :annotation      {:tag ..tag..}
                       :links           {:_collaboration_roots [..root1..]}}

                      {:_id             ..uuid..
                       :entity          ..id2..
                       :user            ..user..
                       :annotation_type ..type..
                       :type            "Annotation"
                       :annotation      {:tag ..tag..}
                       :links           {:_collaboration_roots [..root2..]}}]]

        (a/create-annotations ..auth.. ..rt.. [..id1.. ..id2..] ..type.. [{:tag ..tag..}]) => [..result..]
        (provided
          (util/make-uuid) => ..uuid..
          (auth/authenticated-user-id ..auth..) => ..user..
          (core/get-entities ..auth.. [..id1.. ..id2..] ..rt..) => [{:_id   ..id1..
                                                                     :links {:_collaboration_roots [..root1..]}}
                                                                    {:_id   ..id2..
                                                                     :links {:_collaboration_roots [..root2..]}}]
          (core/create-values ..auth.. ..rt.. expected) => [..result..]
          (a/notify ..auth.. ..result..) => ..result..))))

  (facts "About update-annotation"
    (facts "authorized user"
      (against-background [(auth/authenticated-user-id ..auth..) => ..user..]

        (fact "updates Note :annotation"
          (let [current {:_id             ..uuid..
                         :entity          ..entity..
                         :user            ..user..
                         :annotation_type c/NOTES
                         :type            c/ANNOTATION-TYPE
                         :annotation      {:note ..old..}}]
            (a/update-annotation ..auth.. ..rt.. ..uuid.. {:note ..new..}) => ..result..
            (provided
              (util/iso-short-now) => ..time..
              (core/get-values ..auth.. [..uuid..] :routes ..rt..) => [current]
              (core/update-values ..auth.. ..rt.. [{:_id             ..uuid..
                                                    :entity          ..entity..
                                                    :user            ..user..
                                                    :annotation_type c/NOTES
                                                    :type            c/ANNOTATION-TYPE
                                                    :annotation      {:note ..new..}
                                                    :edited_at       ..time..}]) => [..result..])))


        (fact "raises 422 for non-note annotation"
          (let [tag {:_id             ..uuid..
                     :entity          ..id1..
                     :user            ..user..
                     :annotation_type c/TAGS
                     :type            c/ANNOTATION-TYPE
                     :annotation      {:tag ..tag..}}]
            (a/update-annotation ..auth.. ..rt.. ..uuid.. {:tag ..new..}) => (throws ExceptionInfo)
            (provided
              (core/get-values ..auth.. [..uuid..] :routes ..rt..) => tag)))))

    (facts "unauthorized user"
      (against-background [(auth/authenticated-user-id ..auth..) => ..other..]
        (fact "raises 403 if authenticated user does not own notes"
          (let [tag {:_id             ..uuid..
                     :entity          ..id1..
                     :user            ..user..
                     :annotation_type c/TAGS
                     :type            c/ANNOTATION-TYPE
                     :annotation      {:tag ..tag..}}]
            (a/update-annotation ..auth.. ..rt.. ..uuid.. {:tag ..new..}) => (throws ExceptionInfo)
            (provided
              (core/get-values ..auth.. [..uuid..] :routes ..rt..) => [tag]))))))

  (facts "About `delete-annotations`"
    (fact "calls `delete-values"
      (a/delete-annotations ..auth.. [..id1.. ..id2..] ..rt..) => ..result..
      (provided
        (core/delete-values ..auth.. [..id1.. ..id2..] ..rt..) => ..result..))))

