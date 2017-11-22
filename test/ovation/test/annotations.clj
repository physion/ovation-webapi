(ns ovation.test.annotations
  (:use midje.sweet)
  (:require [ovation.annotations :as a]
            [ovation.util :as util]
            [ovation.db.notes :as notes]
            [ovation.test.db.notes :as tnotes]
            [ovation.db.properties :as properties]
            [ovation.test.db.properties :as tproperties]
            [ovation.db.tags :as tags]
            [ovation.test.db.tags :as ttags]
            [ovation.db.timeline_events :as timeline_events]
            [ovation.test.db.timeline_events :as ttimeline_events]
            [ovation.auth :as auth]
            [ovation.core :as core]
            [ovation.constants :as c]
            [ring.util.http-response :refer [unprocessable-entity forbidden] :as http-response]
            [ovation.constants :as k]
            [ovation.transform.read :as tr])
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
  (let [id1   (str (util/make-uuid))
        id2   (str (util/make-uuid))
        user1 (str (util/make-uuid))
        user2 (str (util/make-uuid))
        a1    (merge
                tnotes/RECORD
                {:entity          id1
                 :user            user1})
        a2    (merge
                tproperties/RECORD
                {:entity          id1
                 :user            user2})
        a3    (merge
                ttags/RECORD
                {:entity          id2
                 :user            user1})
        a4    (merge
                ttimeline_events/RECORD
                {:entity          id2
                 :user            user2})]

    (against-background [(tr/values-from-db [a1 a2 a3 a4] ..ctx..) => ..result..]

      (fact "returns annotation documents grouped by entity and user"
        (a/get-annotations ..ctx.. ..db.. [id1 id2] c/NOTES) => ..result..
        (provided
          (notes/find-all-by-uuid ..db.. anything) => (seq [a1 a2 a3 a4])))

      (fact "returns annotation documents grouped by entity and user"
        (a/get-annotations ..ctx.. ..db.. [id1 id2] c/PROPERTIES) => ..result..
        (provided
          (properties/find-all-by-uuid ..db.. anything) => (seq [a1 a2 a3 a4])))

      (fact "returns annotation documents grouped by entity and user"
        (a/get-annotations ..ctx.. ..db.. [id1 id2] c/TAGS) => ..result..
        (provided
          (tags/find-all-by-uuid ..db.. anything) => (seq [a1 a2 a3 a4])))

      (fact "returns annotation documents grouped by entity and user"
        (a/get-annotations ..ctx.. ..db.. [id1 id2] c/TIMELINE_EVENTS) => ..result..
        (provided
          (timeline_events/find-all-by-uuid ..db.. anything) => (seq [a1 a2 a3 a4]))))))

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
        (auth/authenticated-user-uuid ..auth..) => ..user..
        (core/get-entities ..ctx.. ..db.. [..id1.. ..id2..]) => [entity1
                                                                 entity2]
        (core/create-values ..ctx.. ..db.. expected) => [{:entity ..id1..} {:entity ..id2..}]
        ..ctx.. =contains=> {:ovation.request-context/identity ..auth..}
        (a/notify ..ctx.. entity1 {:entity ..id1..}) => ..result1..
        (a/notify ..ctx.. entity2 {:entity ..id2..}) => ..result2..))))

(facts "About update-annotation"
  (facts "authorized user"
    (against-background [(auth/authenticated-user-uuid ..auth..) => ..user..]

      (fact "updates Note :annotation"
        (let [current {:_id             ..uuid..
                       :entity          ..entity..
                       :user            ..user..
                       :annotation_type c/NOTES
                       :type            c/ANNOTATION-TYPE
                       :annotation      {:note ..old..}}]
          (a/update-annotation ..ctx.. ..db.. ..uuid.. {:note ..new..}) => ..result..
          (provided
            ..ctx.. =contains=> {:ovation.request-context/routes   ..rt..
                                 :ovation.request-context/identity ..auth..}
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
            ..ctx.. =contains=> {:ovation.request-context/routes   ..rt..
                                 :ovation.request-context/identity ..auth..}
            (core/get-values ..ctx.. ..db.. [..uuid..] :routes ..rt..) => [tag])))))

  (facts "unauthorized user"
    (against-background [(auth/authenticated-user-uuid ..auth..) => ..other..]
      (fact "raises 403 if authenticated user does not own notes"
        (let [tag {:_id             ..uuid..
                   :entity          ..id1..
                   :user            ..user..
                   :annotation_type c/TAGS
                   :type            c/ANNOTATION-TYPE
                   :annotation      {:tag ..tag..}}]
          (a/update-annotation ..ctx.. ..db.. ..uuid.. {:tag ..new..}) => (throws ExceptionInfo)
          (provided
            ..ctx.. =contains=> {:ovation.request-context/routes   ..rt..
                                 :ovation.request-context/identity ..auth..}
            (core/get-values ..ctx.. ..db.. [..uuid..] :routes ..rt..) => [tag]))))))

(facts "About `delete-annotations`"
  (fact "calls `delete-values"
    (a/delete-annotations ..ctx.. ..db.. [..id1.. ..id2..]) => ..result..
    (provided
      (core/delete-values ..ctx.. ..db.. [..id1.. ..id2..]) => ..result..)))

