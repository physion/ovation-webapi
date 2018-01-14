(ns ovation.test.mentions
  (:require [midje.sweet :refer :all]
            [ovation.annotations :as a]
            [ovation.core :as core]
            [ovation.auth :as auth]
            [ovation.util :as util]
            [ovation.constants :as c]
            [org.httpkit.fake :refer [with-fake-http]]
            [clojure.data.json :as json]
            [ovation.config :as config]
            [ovation.constants :as k]
            [clojure.string :as string]))


(against-background [..ctx.. =contains=> {:ovation.request-context/identity ..auth..
                                          :ovation.request-context/routes   ..rt..}]

  (facts "About @-mention notification"
    (fact "escapes html"
      (a/sanitized-note-text {:annotation {:text "<body>something</body>"}}) => "&lt;body&gt;something&lt;/body&gt;")
    (fact "does not escape &nbsp;"
      (a/sanitized-note-text {:annotation {:text "something&nbsp;awesome"}}) => "something awesome")

    (facts "send-mention-notification"
      (fact "POSTs notification"
        (let [entity-id     (str (util/make-uuid))
              annotation-id (str (util/make-uuid))
              user-id       (str (util/make-uuid))
              server        config/NOTIFICATIONS_SERVER
              text          "some text"
              entity-type   k/PROJECT-TYPE
              body          {:notification {:url (str entity-type "://" (util/join-path [entity-id annotation-id]))}}]
          (with-fake-http [{:url (util/join-path [server "api" "common" "v1" "notifications"]) :method :post} {:body   (json/write-str body)
                                                                                                               :status 201}]
            (:status @(a/send-mention-notification ..ctx.. user-id {:_id   entity-id
                                                                     :type entity-type} annotation-id text)) => 201)))
      (fact "sets :url"
        (let [entity-id     (str (util/make-uuid))
              annotation-id (str (util/make-uuid))
              user-id       (str (util/make-uuid))
              proj-id       (str (util/make-uuid))
              org-id        ..org..
              text          "some text"
              entity-type   k/FOLDER-TYPE
              entity        {:_id   entity-id
                             :type  entity-type
                             :links {:_collaboration_roots [proj-id]}}]
          (a/mention-notification-body org-id user-id entity annotation-id text) => {:user_id                user-id
                                                                                     :organization_id        ..org..
                                                                                     :url                    (str (string/lower-case entity-type) "://" proj-id "/" entity-id "/" annotation-id)
                                                                                     :notification_type      k/MENTION_NOTIFICATION
                                                                                     :body                   text})))

    (facts "create-annotations"
      (fact "notifies"
        (let [note     {:_id             ..uuid..
                        :entity          ..id1..
                        :user            ..user..
                        :annotation_type ..type..
                        :type            "Annotation"
                        :annotation      {:tag ..tag..}
                        :links           {:_collaboration_roots [..root1..]}}
              expected [note]
              entity   {:_id   ..id1..
                        :type  k/FILE-TYPE
                        :links {:_collaboration_roots [..root1..]}}]

          (a/create-annotations ..ctx.. ..db.. [..id1..] ..type.. [{:tag ..tag..}]) => [..notified..]
          (provided
            (util/make-uuid) => ..uuid..
            (auth/authenticated-user-uuid ..auth..) => ..user..
            (core/get-entities ..ctx.. ..db.. [..id1..]) => [entity]
            (core/create-values ..ctx.. ..db.. expected) => [note]
            (a/notify ..ctx.. entity note) => ..notified..))))

    (facts "update-annotations"
      (against-background [(auth/authenticated-user-uuid ..auth..) => ..user..]
        (fact "notifies"
          (let [current {:_id             ..uuid..
                         :entity          ..entity..
                         :user            ..user..
                         :annotation_type c/NOTES
                         :type            c/ANNOTATION-TYPE
                         :annotation      {:text ..old..}}
                entity  {:_id   ..entity..
                         :type  k/FILE-TYPE
                         :links {:_collaboration_roots [..root1..]}}]

            (a/update-annotation ..ctx.. ..db.. ..uuid.. {:text ..new..}) => ..result..

            (provided
              (util/iso-now) => ..time..
              (core/get-values ..ctx.. ..db.. [..uuid..]) =streams=> [[current] [..result..]]
              (core/get-entities ..ctx.. ..db.. [..entity..]) => [entity]
              (core/update-values ..ctx.. ..db.. [{:_id             ..uuid..
                                                   :entity          ..entity..
                                                   :user            ..user..
                                                   :annotation_type c/NOTES
                                                   :type            c/ANNOTATION-TYPE
                                                   :annotation      {:text ..new..}
                                                   :edited_at       ..time..}]) => [..result..]
              (a/notify ..ctx.. entity ..result..) => ..notified..)))))


    (facts "notify"
      (fact "sends notification"
        (let [entity-id (str (util/make-uuid))
              note-id   (str (util/make-uuid))
              note      {:type            c/ANNOTATION-TYPE
                         :_id             note-id
                         :entity          (str "project://" entity-id "/" note-id)
                         :annotation_type c/NOTES
                         :annotation      {:text "text"}}
              user-id   (str (util/make-uuid))]
          (a/notify ..ctx.. ..entity.. note) => note
          (provided
            ..entity.. =contains=> {:type k/PROJECT-TYPE
                                    :_id  entity-id}
            (a/mentions note) => [{:name ..name.. :uuid user-id}]
            (a/send-mention-notification ..ctx.. user-id ..entity.. note-id "text") => []))))

    (facts "notified-users"
      (fact "finds notified users"
        (let [text "{{user-mention uuid=1}}Barry{{/user-mention}} foo bar baz {{user-mention uuid=2222-3333}}Rens{{/user-mention}}"]
          (a/mentions {:type            c/ANNOTATION-TYPE
                       :entity          ..entity..
                       :annotation_type c/NOTES
                       :annotation      {:text text}}) => [{:name "Barry", :uuid "1"} {:name "Rens", :uuid "2222-3333"}])))))
