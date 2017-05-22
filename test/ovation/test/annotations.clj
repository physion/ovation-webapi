(ns ovation.test.annotations
  (:use midje.sweet)
  (:require [ovation.annotations :as a]
            [ovation.util :as util]
            [ovation.couch :as couch]
            [ovation.auth :as auth]
            [ovation.core :as core]
            [ovation.constants :as c]
            [ring.util.http-response :refer [unprocessable-entity forbidden] :as http-response]
            [ovation.constants :as k]
            [ovation.transform.read :as read])
  (:import (clojure.lang ExceptionInfo)))


(facts "About entity-uri"
  (fact "for project"
    (let [proj-id (str (util/make-uuid))]
      (a/entity-uri {:_id proj-id :type k/PROJECT-TYPE}) => (str "project://" proj-id)))

  (fact "for nested entity"
    (let [proj-id   (str (util/make-uuid))
          entity-id (str (util/make-uuid))]
      (a/entity-uri {:_id   entity-id
                     :type  k/FILE-TYPE
                     :links {:_collaboration_roots [proj-id]}}) => (str "file://" proj-id "/" entity-id))))

(facts "About `get-annotations`"
  (fact "returns annotation documents grouped by entity and user"
    (let [id1   (str (util/make-uuid))
          id2   (str (util/make-uuid))
          user1 (str (util/make-uuid))
          user2 (str (util/make-uuid))
          a1    {:type            "Annotation"
                 :annotation_type ..type..
                 :_rev            ..rev..
                 :entity          id1
                 :user            user1}
          a2    {:type            "Annotation"
                 :annotation_type ..type..
                 :_rev            ..rev..
                 :entity          id1
                 :user            user2}
          a3    {:type            "Annotation"
                 :annotation_type ..type..
                 :_rev            ..rev..
                 :entity          id2
                 :user            user1}
          a4    {:type            "Annotation"
                 :annotation_type ..type..
                 :_rev            ..rev..
                 :entity          id2
                 :user            user2}]
      (a/get-annotations ..ctx.. ..db.. [id1 id2] ..type..) => ..result..
      ;{(keyword id1) {(keyword user1) [a1]
      ;                                                                   (keyword user2) [a2]}
      ;                                                    (keyword id2) {(keyword user1) [a3]
      ;                                                                   (keyword user2) [a4]}}
      (provided
        (read/values-from-couch [a1 a2 a3 a4] ..ctx..) => ..result..
        (couch/get-view ..ctx.. ..db.. "annotation_docs" {:keys         [[id1 ..type..]
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
                     :links           {:_collaboration_roots [..root2..]}}]
          entity1  {:_id   ..id1..
                    :type  ..type..
                    :links {:_collaboration_roots [..root1..]}}

          entity2  {:_id   ..id2..
                    :type  ..type..
                    :links {:_collaboration_roots [..root2..]}}]

      (a/create-annotations ..ctx.. ..db.. [..id1.. ..id2..] ..type.. [{:tag ..tag..}]) => [..result1.. ..result2..]
      (provided
        (util/make-uuid) => ..uuid..
        (auth/authenticated-user-id ..auth..) => ..user..
        (core/get-entities ..ctx.. ..db.. [..id1.. ..id2..]) => [entity1
                                                                 entity2]
        (core/create-values ..ctx.. ..db.. expected) => [{:entity ..id1..} {:entity ..id2..}]
        ..ctx.. =contains=> {:ovation.request-context/auth ..auth..}
        (a/notify ..auth.. entity1 {:entity ..id1..}) => ..result1..
        (a/notify ..auth.. entity2 {:entity ..id2..}) => ..result2..))))

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
          (a/update-annotation ..ctx.. ..db.. ..uuid.. {:note ..new..}) => ..result..
          (provided
            ..ctx.. =contains=> {:ovation.request-context/routes ..rt..
                                 :ovation.request-context/auth   ..auth..}
            (util/iso-short-now) => ..time..
            (core/get-values ..ctx.. ..db.. [..uuid..] :routes ..rt..) => [current]
            (core/get-entities ..ctx.. ..db.. [..entity..]) => {:_id  ..entity..
                                                                :type k/PROJECT-TYPE}
            (core/update-values ..ctx.. ..db.. [{:_id             ..uuid..
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
          (a/update-annotation ..ctx.. ..db.. ..uuid.. {:tag ..new..}) => (throws ExceptionInfo)
          (provided
            ..ctx.. =contains=> {:ovation.request-context/routes ..rt..
                                 :ovation.request-context/auth   ..auth..}
            (core/get-values ..ctx.. ..db.. [..uuid..] :routes ..rt..) => [tag])))))

  (facts "unauthorized user"
    (against-background [(auth/authenticated-user-id ..auth..) => ..other..]
      (fact "raises 403 if authenticated user does not own notes"
        (let [tag {:_id             ..uuid..
                   :entity          ..id1..
                   :user            ..user..
                   :annotation_type c/TAGS
                   :type            c/ANNOTATION-TYPE
                   :annotation      {:tag ..tag..}}]
          (a/update-annotation ..ctx.. ..db.. ..uuid.. {:tag ..new..}) => (throws ExceptionInfo)
          (provided
            ..ctx.. =contains=> {:ovation.request-context/routes ..rt..
                                 :ovation.request-context/auth   ..auth..}
            (core/get-values ..ctx.. ..db.. [..uuid..] :routes ..rt..) => [tag]))))))

(facts "About `delete-annotations`"
  (fact "calls `delete-values"
    (a/delete-annotations ..ctx.. ..db.. [..id1.. ..id2..]) => ..result..
    (provided
      (core/delete-values ..ctx.. ..db.. [..id1.. ..id2..]) => ..result..)))

