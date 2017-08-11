(ns ovation.test.auth
  (:use midje.sweet)
  (:require [ovation.auth :as auth]
            [ovation.teams :as teams]
            [clojure.core.async :as async :refer [>!!]]
            [ovation.request-context :as request-context]
            [org.httpkit.fake :refer [with-fake-http]]
            [clojure.data.codec.base64 :as b64]
            [ovation.util :as util]
            [clojure.data.json :as json]
            [ovation.config :as config]
            [ovation.constants :as k]
            [ovation.core :as core]
            [ovation.middleware.auth :as middleware.auth]
            [ovation.authz :as authz])
  (:import (clojure.lang ExceptionInfo)
           (java.util UUID)))


(defn string-to-base64-string [original]
  (String. (b64/encode (.getBytes original)) "UTF-8"))

(facts "About authentication"
  (fact "`authorized-user-id` returns user UUID"
    (auth/authenticated-user-id ..auth..) => ..id..
    (provided
      ..auth.. =contains=> {::auth/authenticated-teams (future {:user_uuid ..id..})})))

(facts "About `organization-ids`"
  (fact "returns organization-ids from authenticated-teams"
    (auth/organization-ids ..auth..) => ..ids..
    (provided
      ..auth.. =contains=> {::auth/authenticated-teams (future {:organization_ids ..ids..})})))

(facts "About `service-account?`"
  (fact "with ::service-account true"
    (auth/service-account? {::auth/service-account true}) => true)
  (fact "with ::service-account false"
    (auth/service-account? {::auth/service-account false}) => false)
  (fact "with ::service-account nil"
    (auth/service-account? {}) => false))

(facts "About `middleware.auth.service-sub?`"
  (fact "recognizes user accounts"
    (middleware.auth/service-sub? {:sub (str (util/make-uuid))}) => false)
  (fact "recognizes service accounts"
    (middleware.auth/service-sub? {:sub (format "%s@clients" (util/make-uuid))}) => true)
  (fact "recognizes identity without sub as non-service account"
    (middleware.auth/service-sub? {}) => false))

(facts "About `authenticated-service-account?"
  (fact "requires service account"
    (auth/authenticated-service-account? ..req..) => true
    (provided
      ..req.. =contains=> {:identity {::auth/service-account true}}))
  (fact "rejects user account"
    (auth/authenticated-service-account? ..req..) => false
    (provided
      ..req.. =contains=> {:identity {::auth/service-account false}})))

(facts "About `authenticated-user-account?"
  (fact "rejects service account"
    (auth/authenticated-user-account? ..req..) => false
    (provided
      ..req.. =contains=> {:identity {::auth/service-account true}}))
  (fact "requires user account"
    (auth/authenticated-user-account? ..req..) => true
    (provided
      ..req.. =contains=> {:identity {::auth/service-account false}})))

(facts "About `middleware.auth.scopes`"
  (facts "parsing"
    (fact "for :scope"
      (middleware.auth/scopes {:scope  "scope1 scope2"}) => ["scope1" "scope2"])
    (fact "for :https://ovation.io/scope"
      (middleware.auth/scopes {(keyword "https://ovation.io/scope") "scope1 scope2"}) => ["scope1" "scope2"])))

(facts "About has-scope?"
  (fact "with scope"
    (auth/has-scope? {::auth/scopes ["read:global foo:bar"]} "read:global") => true)
  (fact "without scope"
    (auth/has-scope? {::auth/scopes ["read:global foo:bar"]} "write:global") => false)
  (fact "with wildcard scope"
    (auth/has-scope? {::auth/scopes ["read:global foo:bar"]} "read:*") => true)
  (fact "without wildcard scope"
    (auth/has-scope? {::auth/scopes ["foo:bar"]} "read:*") => false))

(facts "About `can?`"
  (fact "returns true when auth.identity is a service account"
    ;; When it's a service account, authz is handled via scopes at the handler
    (auth/can? ..ctx.. ::auth/read {:type  "Project"
                                    :owner ..user..}) => true
    (provided
      ..ctx.. =contains=> {::request-context/identity {::auth/service-account true}}))

  (fact "throws not-found! if authenticated organization not in authenticated teams"
    (auth/can? ..ctx.. ::auth/read {:type  "Project"
                                    :owner ..user..}) => (throws ExceptionInfo)
    (provided
      (auth/organization-ids ..auth..) => [..org2..]
      ..ctx.. =contains=> {::request-context/identity ..auth..
                           ::request-context/org      ..org..}))

  (facts ":read"
    (against-background [(auth/authenticated-user-id ..auth..) => ..user..
                         (auth/authenticated-teams ..auth..) => [..team1.. ..team2..]
                         (auth/organization-ids ..auth..) => [..org..]
                         ..ctx.. =contains=> {::request-context/identity ..auth..
                                              ::request-context/org      ..org..}
                         (auth/has-scope? ..auth.. k/READ-GLOBAL-SCOPE) => true]
      (facts "entities"
        (fact "allowed when user is owner"
          (auth/can? ..ctx.. ::auth/read {:type   "Project"
                                           :owner ..user..}) => true)
        (fact "allowed when one of authenticated user's teams in collaboration roots"
          (auth/can? ..ctx.. ::auth/read {:type   "File"
                                           :owner ..other..
                                           :links {:_collaboration_roots [..team1.. ..team3..]}}) => true)
        (fact "allowed when all of authenticated user's teams in collaboration roots"
          (auth/can? ..ctx.. ::auth/read {:type   "File"
                                           :owner ..other..
                                           :links {:_collaboration_roots [..team1.. ..team2..]}}) => true)

        (fact "not allowed when not owner or team member"
          (auth/can? ..ctx.. ::auth/read {:type   "File"
                                           :owner ..other..
                                           :links {:_collaboration_roots [..team3..]}}) => false)

        (fact "not allowed when not owner"
          (auth/can? ..ctx.. ::auth/read {:type   "File"
                                           :owner ..other..
                                           :links {:_collaboration_roots []}}) => false))

      (facts "annotations"
        (fact "not allowed when not owner"
          (auth/can? ..ctx.. ::auth/read {:type    "Annotation"
                                           :user   ..other..
                                           :entity ..id..
                                           :links  {:_collaboration_roots []}}) => false)
        (fact "not allowed when none of authenticated user's teams in collaboration roots"
          (auth/can? ..ctx.. ::auth/read {:type    "Annotation"
                                           :user   ..other..
                                           :entity ..id..
                                           :links  {:_collaboration_roots [..team3..]}}) => false)
        (fact "allowed when user is :user"
          (auth/can? ..ctx.. ::auth/read {:type    "Annotation"
                                           :user   ..user..
                                           :entity ..id..}) => true)
        (fact "allowed when one of authenticated user's teams in collaboration roots"
          (auth/can? ..ctx.. ::auth/read {:type    "Annotation"
                                           :user   ..other..
                                           :entity ..id..
                                           :links  {:_collaboration_roots [..team1.. ..team3..]}}) => true)
        (fact "allowed when all of authenticated user's teams in collaboration roots"
          (auth/can? ..ctx.. ::auth/read {:type    "Annotation"
                                           :user   ..other..
                                           :entity ..id..
                                           :links  {:_collaboration_roots [..team1.. ..team2..]}}) => true))
      (facts "relationships"
        (fact "not allowed when user is not :user"
          (auth/can? ..ctx.. ::auth/read {:type     "Relation"
                                           :user_id ..other..}) => false)
        (fact "not allowed when user is not user or team member"
          (auth/can? ..ctx.. ::auth/read {:type     "Relation"
                                           :user_id ..other..
                                           :links   {:_collaboration_roots [..team3..]}}) => false)
        (fact "allowed when user is :user"
          (auth/can? ..ctx.. ::auth/read {:type     "Relation"
                                           :user_id ..user..}) => true)
        (fact "allowed when one of authenticated user's teams in collaboration roots"
          (auth/can? ..ctx.. ::auth/read {:type     "Relation"
                                           :user_id ..user..
                                           :links   {:_collaboration_roots [..team1.. ..team3..]}}) => true)
        (fact "allowed when all of authenticated user's teams in collaboration roots"
          (auth/can? ..ctx.. ::auth/read {:type     "Relation"
                                           :user_id ..user..
                                           :links   {:_collaboration_roots [..team1.. ..team2..]}}) => true))))

  (facts ":create"
    (against-background [(auth/authenticated-user-id ..auth..) => ..user..
                         (auth/organization-ids ..auth..) => [..org..]
                         ..ctx.. =contains=> {::request-context/identity ..auth..
                                              ::request-context/org      ..org..}
                         (auth/has-scope? ..auth.. k/WRITE-GLOBAL-SCOPE) => true]
      (facts "Annotations"
        (fact "allowed when :user is authenticated user and can read all roots"
          (auth/can? ..ctx.. ::auth/create {:type    "Annotation"
                                             :user   ..user..
                                             :entity ..id..}) => true)
          ;(provided
          ;  (auth/get-permissions ..auth.. [..id..]) => ..permissions..
          ;  (auth/collect-permissions ..permissions.. :read) => [true])


        (fact "denied when :user is not authenticated user but can read all roots"
          (auth/can? ..ctx.. ::auth/create {:type    "Annotation"
                                             :user   ..other..
                                             :entity ..id..}) => false))

        ;(fact "denied when :user is authenticated user but cannot read any roots"
      ;  (auth/can? ..ctx.. ::auth/create {:type "Annotation"
        ;                                     :user ..user..
        ;                                     :entity ..id..}) => false
        ;  (provided
        ;    (auth/get-permissions ..auth.. [..id..]) => ..permissions..
        ;    (auth/collect-permissions ..permissions.. :read) => [false false]))


      (facts "Relations"
        (fact "allowed when :user is authenticated user and can read source and target"
          (auth/can? ..ctx.. ::auth/create {:type       "Relation"
                                             :user_id   ..user..
                                             :source_id ..src..
                                             :target_id ..target..}) => true))
          ;(provided
          ;  (auth/get-permissions ..auth.. [..src.. ..target..]) => ..perms..
          ;  (auth/collect-permissions ..perms.. :read) => [true true])


        ;(fact "denied when :user is authenticated user and cannot read source and target"
      ;  (auth/can? ..ctx.. ::auth/create {:type "Relation"
        ;                                     :user_id ..user..
        ;                                     :source_id ..src..
        ;                                     :target_id ..target..}) => true
        ;  (provided
        ;    (auth/get-permissions ..auth.. [..src.. ..target..]) => ..perms..
        ;    (auth/collect-permissions ..perms.. :read) => [true false]))



      (facts "projects"
        (fact "allow when :owner nil"
          (auth/can? ..ctx.. ::auth/create {:type   "Project"
                                             :owner nil}) => true)
        (fact "allow when :owner is current user"
          (auth/can? ..ctx.. ::auth/create {:type   "Project"
                                             :owner ..user..}) => true))

      (facts "entities"
        (fact "allow when :owner nil and can read all roots"
          (auth/can? ..ctx.. ::auth/create {:type   "Entity"
                                             :owner nil
                                             :links {:_collaboration_roots [..root..]}}) => true
          (provided
            (auth/authenticated-user-id ..auth..) => ..user..
            (auth/permissions ..auth.. [..root..]) => [{:uuid            ..root..
                                                            :permissions {:read true}}]))

        (fact "denies when :owner nil and cannot read all roots"
          (auth/can? ..ctx.. ::auth/create {:type   "Entity"
                                             :owner nil
                                             :links {:_collaboration_roots [..root..]}}) => falsey
          (provided
            (auth/permissions ..auth.. [..root..]) => [{:uuid            ..root..
                                                            :permissions {:read false}}
                                                       {:uuid        ..root2..
                                                        :permissions {:read true}}]))


        (fact "allows when :owner is auth user and can read all roots"
          (auth/can? ..ctx.. ::auth/create {:type   "Entity"
                                             :owner ..user..
                                             :links {:_collaboration_roots [..root..]}}) => true
          (provided
            (auth/permissions ..auth.. [..root..]) => ..permissions..
            (auth/collect-permissions ..permissions.. :read) => [true]))

        (fact "denies when :owner is auth user and cannot read all roots"
          (auth/can? ..ctx.. ::auth/create {:type   "Entity"
                                             :owner ..user..
                                             :links {:_collaboration_roots [..root..]}}) => false
          (provided
            (auth/authenticated-user-id ..auth..) => ..user..
            (auth/permissions ..auth.. [..root..]) => ..permissions..
            (auth/collect-permissions ..permissions.. :read) => [true false])))))

  (facts ":update"
    (against-background [(auth/authenticated-user-id ..auth..) => (str (UUID/randomUUID))
                         (auth/organization-ids ..auth..) => [..org..]
                         ..ctx.. =contains=> {::request-context/identity ..auth..
                                              ::request-context/org      ..org..}
                         (auth/has-scope? ..auth.. k/WRITE-GLOBAL-SCOPE) => true]
      (fact "Delegates to get-permissions when not owner"
        (auth/can? ..ctx.. ::auth/update {:type   "Entity"
                                           :owner (str (UUID/randomUUID))
                                           :links {:_collaboration_roots ..roots..}}) => true
        (provided
          (auth/permissions ..auth.. ..roots..) => [{:uuid            :uuid1
                                                         :permissions {:read  true
                                                                       :write false
                                                                       :admin false}}
                                                    {:uuid        :uuid2
                                                     :permissions {:read  true
                                                                   :write true
                                                                   :admin false}}]))))
  (facts ":delete"
    (against-background [(auth/authenticated-user-id ..auth..) => ..user..
                         (auth/organization-ids ..auth..) => [..org..]
                         ..ctx.. =contains=> {::request-context/identity ..auth..
                                              ::request-context/org      ..org..}
                         (auth/has-scope? ..auth.. k/WRITE-GLOBAL-SCOPE) => true]
      (facts "Relations"
        (fact "allowed if user is owner"
          (auth/can? ..ctx.. ::auth/delete {:type     k/RELATION-TYPE
                                             :user_id ..user..}) => true)
        (fact "allowed if user can admin all collaboration roots"
          (let [doc {:type k/RELATION-TYPE
                     :user_id ..other..
                     :source_id ..src..
                     :target_id ..target..
                     :links {:_collaboration_roots ..roots..}}]
            (auth/can? ..ctx.. ::auth/delete doc) => true
            (provided
              (auth/permissions ..auth.. ..roots..) => ..permissions..
              (auth/collect-permissions ..permissions.. :write) => [true true]))))
      (fact "Annoations require :user match authenticated user"
        (auth/can? ..ctx.. ::auth/delete {:type  "Annotation"
                                           :user ..user..}) => true
        (auth/can? ..ctx.. ::auth/delete {:type  "Annotation"
                                           :user ..other..}) => false)

      (fact "allowed when entity :owner is nil"
        (auth/can? ..ctx.. ::auth/delete {:type   "Entity"
                                           :owner nil}) => true
        (provided
          (auth/permissions ..auth.. nil) => {}))

      (fact "allowed when :write on all roots when not owner"
        (auth/can? ..ctx.. ::auth/delete {:type   "Entity"
                                           :owner (str (UUID/randomUUID))
                                           :links {:_collaboration_roots ..roots..}}) => true
        (provided
          (auth/authenticated-user-id ..auth..) => (str (UUID/randomUUID))
          (auth/permissions ..auth.. ..roots..) => [{:uuid            :uuid1
                                                         :permissions {:read  true
                                                                       :write true
                                                                       :admin true}}
                                                    {:uuid        :uuid2
                                                     :permissions {:read  true
                                                                   :write true
                                                                   :admin false}}]))

      (fact "Requires :write on all roots when not owner"
        (auth/can? ..ctx.. ::auth/delete {:type   "Entity"
                                           :owner (str (UUID/randomUUID))
                                           :links {:_collaboration_roots ..roots..}}) => false
        (provided
          (auth/authenticated-user-id ..auth..) => (str (UUID/randomUUID))
          (auth/permissions ..auth.. ..roots..) => {:permissions [{:uuid            :uuid1
                                                                       :permissions {:read  true
                                                                                     :write false
                                                                                     :admin false}}
                                                                  {:uuid        :uuid2
                                                                   :permissions {:read  true
                                                                                 :write false
                                                                                 :admin true}}]})))))

(facts "About permissions"
  (fact "gets permissions from auth server"
    (let [uuids  [(str (UUID/randomUUID)) (str (UUID/randomUUID))]
          body   {:permissions [{:uuid        (first uuids)
                                 :permissions {}}
                                {:uuid        (last uuids)
                                 :permissions {}}]}
          server config/AUTH_SERVER
          auth   {:server server}]
      (with-fake-http [{:url (util/join-path [server "api" "v2" "permissions"]) :method :get} {:body   (json/write-str body)
                                                                                               :status 200}]
        @(auth/get-permissions auth) => (:permissions body)))))

(facts "About `get-permissions`"
  (fact "gets permissions from future stored in authenticated identity"
    (let [uuids              [(str (UUID/randomUUID)) (str (UUID/randomUUID))]
          permissions        [{:uuid        (first uuids)
                               :permissions ..permisions..}
                              {:uuid        (last uuids)
                               :permissions ..other-permissions..}]
          future-permissions (promise)]
      (deliver future-permissions permissions)
      (auth/permissions ..auth.. [(first uuids)]) => [{:uuid            (first uuids)
                                                           :permissions ..permisions..}]
      (provided
        ..auth.. =contains=> {::auth/authenticated-permissions future-permissions}))))


(facts "About `collect-permissions"
  (fact "gets permissions"
    (auth/collect-permissions [{:uuid        :uuid1
                                :permissions {:read  false
                                              :write false
                                              :admin true}}
                               {:uuid        :uuid2
                                :permissions {:read  true
                                              :write true
                                              :admin false}}]
      :read) => [false true]))

