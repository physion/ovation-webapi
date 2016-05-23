(ns ovation.test.mentions
  (:require [midje.sweet :refer :all]
            [ovation.annotations :as a]
            [ovation.core :as core]
            [ovation.auth :as auth]
            [ovation.util :as util]
            [ovation.constants :as c]))


(facts "About @-mention notification"
  (facts "create-annotations"
    (fact "notifies"
      (let [expected [{:_id             ..uuid..
                       :entity          ..id1..
                       :user            ..user..
                       :annotation_type ..type..
                       :type            "Annotation"
                       :annotation      {:tag ..tag..}
                       :links           {:_collaboration_roots [..root1..]}}]]

        (a/create-annotations ..auth.. ..rt.. [..id1..] ..type.. [{:tag ..tag..}]) => ..notified..
        (provided
          (util/make-uuid) => ..uuid..
          (auth/authenticated-user-id ..auth..) => ..user..
          (core/get-entities ..auth.. [..id1..] ..rt..) => [{:_id   ..id1..
                                                             :links {:_collaboration_roots [..root1..]}}]
          (core/create-values ..auth.. ..rt.. expected) => ..result..
          (a/notify ..result..) => ..notified..))))

  (facts "update-annotations"
    (against-background [(auth/authenticated-user-id ..auth..) => ..user..]
      (fact "notifies"
        (let [current {:_id             ..uuid..
                       :entity          ..entity..
                       :user            ..user..
                       :annotation_type c/NOTES
                       :type            c/ANNOTATION-TYPE
                       :annotation      {:note ..old..}}]

          (a/update-annotation ..auth.. ..rt.. ..uuid.. {:note ..new..}) => ..notified..

          (provided
            (util/iso-short-now) => ..time..
            (core/get-values ..auth.. [..uuid..] :routes ..rt..) => [current]
            (core/update-values ..auth.. ..rt.. [{:_id             ..uuid..
                                                  :entity          ..entity..
                                                  :user            ..user..
                                                  :annotation_type c/NOTES
                                                  :type            c/ANNOTATION-TYPE
                                                  :annotation      {:note ..new..}
                                                  :edited_at       ..time..}]) => [..result..]
            (a/notify [..result..]) => [..notified..])))))


  (facts "notify"
    (fact "sends notification"
      (let [note {:type c/ANNOTATION-TYPE
                  :entity (str (util/make-uuid))
                  :annotation_type c/NOTES
                  :annotation {:note "text"}}]
        (a/notify note) => note
        (provided
          (a/mentions note) => [{:name ..name.. :uuid (str (util/make-uuid))}]))))

  (facts "notified-users"
    (fact "finds notified users"
      (let [text "<user-mention uuid=1>Barry</user-mention> foo bar baz <user-mention uuid=2>Rens</user-mention>"]
        (a/mentions {:type                  c/ANNOTATION-TYPE
                           :entity          ..entity..
                           :annotation_type c/NOTES
                           :annotation      {:note text}}) => [{:name "Barry", :uuid "1"} {:name "Rens", :uuid "2"}]))))
