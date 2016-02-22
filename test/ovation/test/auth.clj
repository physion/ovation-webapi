(ns ovation.test.auth
  (:use midje.sweet)
  (:require [ovation.auth :as auth]
            [ovation.teams :as teams]
            [clojure.core.async :as async :refer [>!!]]
            [org.httpkit.fake :refer [with-fake-http]]
            [clojure.data.codec.base64 :as b64]
            [ovation.util :as util]
            [clojure.data.json :as json]
            [ovation.config :as config])
  (:import (clojure.lang ExceptionInfo)
           (java.util UUID)))


(defn string-to-base64-string [original]
  (String. (b64/encode (.getBytes original)) "UTF-8"))

(facts "About authentication"
  (fact "`authorized-user-id` returns user UUID"
    (auth/authenticated-user-id ...auth...) => ...id...
    (provided
      ...auth... =contains=> {:uuid ...id...})))


(facts "About `can?`"
  (facts ":read"
    (against-background [(auth/authenticated-user-id ..auth..) => ..user..
                         (auth/authenticated-teams ..auth..) => [..team1.. ..team2..]]
      (facts "entities"
        (fact "allowed when user is owner"
          (auth/can? ..auth.. ::auth/read {:type "Project"
                                           :owner ..user..}) => true)
        (fact "allowed when one of authenticated user's teams in collaboration roots"
          (auth/can? ..auth.. ::auth/read {:type "File"
                                           :owner ..other..
                                           :links {:_collaboration_roots [..team1.. ..team3..]}}) => true)
        (fact "allowed when all of authenticated user's teams in collaboration roots"
          (auth/can? ..auth.. ::auth/read {:type "File"
                                           :owner ..other..
                                           :links {:_collaboration_roots [..team1.. ..team2..]}}) => true)

        (fact "not allowed when not owner or team member"
          (auth/can? ..auth.. ::auth/read {:type "File"
                                           :owner ..other..
                                           :links {:_collaboration_roots [..team3..]}}) => false)

        (fact "not allowed when not owner"
          (auth/can? ..auth.. ::auth/read {:type "File"
                                           :owner ..other..
                                           :links {:_collaboration_roots []}}) => false))

      (facts "annotations"
        (fact "not allowed when not owner"
          (auth/can? ..auth.. ::auth/read {:type "Annotation"
                                           :user ..other..
                                           :entity ..id..
                                           :links {:_collaboration_roots []}}) => false)
        (fact "not allowed when none of authenticated user's teams in collaboration roots"
          (auth/can? ..auth.. ::auth/read {:type "Annotation"
                                           :user ..other..
                                           :entity ..id..
                                           :links {:_collaboration_roots [..team3..]}}) => false)
        (fact "allowed when user is :user"
          (auth/can? ..auth.. ::auth/read {:type "Annotation"
                                           :user ..user..
                                           :entity ..id..}) => true)
        (fact "allowed when one of authenticated user's teams in collaboration roots"
          (auth/can? ..auth.. ::auth/read {:type "Annotation"
                                           :user ..other..
                                           :entity ..id..
                                           :links {:_collaboration_roots [..team1.. ..team3..]}}) => true)
        (fact "allowed when all of authenticated user's teams in collaboration roots"
          (auth/can? ..auth.. ::auth/read {:type "Annotation"
                                           :user ..other..
                                           :entity ..id..
                                           :links {:_collaboration_roots [..team1.. ..team2..]}}) => true))
      (facts "relationships"
        (fact "not allowed when user is not :user"
          (auth/can? ..auth.. ::auth/read {:type "Relation"
                                           :user_id ..other..}) => false)
        (fact "not allowed when user is not user or team member"
          (auth/can? ..auth.. ::auth/read {:type "Relation"
                                           :user_id ..other..
                                           :links {:_collaboration_roots [..team3..]}}) => false)
        (fact "allowed when user is :user"
          (auth/can? ..auth.. ::auth/read {:type "Relation"
                                           :user_id ..user..}) => true)
        (fact "allowed when one of authenticated user's teams in collaboration roots"
          (auth/can? ..auth.. ::auth/read {:type "Relation"
                                           :user_id ..user..
                                           :links {:_collaboration_roots [..team1.. ..team3..]}}) => true)
        (fact "allowed when all of authenticated user's teams in collaboration roots"
          (auth/can? ..auth.. ::auth/read {:type "Relation"
                                           :user_id ..user..
                                           :links {:_collaboration_roots [..team1.. ..team2..]}}) => true))))

  (facts ":create"
    (against-background [(auth/authenticated-user-id ..auth..) => ..user..]
      (facts "Annotations"
        (fact "allowed when :user is authenticated user and can read all roots"
          (auth/can? ..auth.. ::auth/create {:type "Annotation"
                                             :user ..user..
                                             :entity ..id..}) => true
          ;(provided
          ;  (auth/get-permissions ..auth.. [..id..]) => ..permissions..
          ;  (auth/collect-permissions ..permissions.. :read) => [true])
          )

        (fact "denied when :user is not authenticated user but can read all roots"
          (auth/can? ..auth.. ::auth/create {:type "Annotation"
                                             :user ..other..
                                             :entity ..id..}) => false)

        ;(fact "denied when :user is authenticated user but cannot read any roots"
        ;  (auth/can? ..auth.. ::auth/create {:type "Annotation"
        ;                                     :user ..user..
        ;                                     :entity ..id..}) => false
        ;  (provided
        ;    (auth/get-permissions ..auth.. [..id..]) => ..permissions..
        ;    (auth/collect-permissions ..permissions.. :read) => [false false]))
        )

      (facts "Relations"
        (fact "allowed when :user is authenticated user and can read source and target"
          (auth/can? ..auth.. ::auth/create {:type "Relation"
                                             :user_id ..user..
                                             :source_id ..src..
                                             :target_id ..target..}) => true
          ;(provided
          ;  (auth/get-permissions ..auth.. [..src.. ..target..]) => ..perms..
          ;  (auth/collect-permissions ..perms.. :read) => [true true])
          )

        ;(fact "denied when :user is authenticated user and cannot read source and target"
        ;  (auth/can? ..auth.. ::auth/create {:type "Relation"
        ;                                     :user_id ..user..
        ;                                     :source_id ..src..
        ;                                     :target_id ..target..}) => true
        ;  (provided
        ;    (auth/get-permissions ..auth.. [..src.. ..target..]) => ..perms..
        ;    (auth/collect-permissions ..perms.. :read) => [true false]))
        )


      (facts "projects"
        (fact "allow when :owner nil"
          (auth/can? ..auth.. ::auth/create {:type "Project"
                                             :owner nil}) => true)
        (fact "allow when :owner is current user"
          (auth/can? ..auth.. ::auth/create {:type "Project"
                                             :owner ..user..}) => true))

      (facts "entities"
        (fact "allow when :owner nil and can read all roots"
          (auth/can? ..auth.. ::auth/create {:type  "Entity"
                                             :owner nil
                                             :links {:_collaboration_roots [..root..]}}) => true
          (provided
            (auth/authenticated-user-id ..auth..) => ..user..
            (auth/get-permissions ..auth.. [..root..]) => {:permissions [{:uuid        ..root..
                                                                          :permissions {:read true}}]}))

        (fact "denies when :owner nil and cannot read all roots"
          (auth/can? ..auth.. ::auth/create {:type  "Entity"
                                             :owner nil
                                             :links {:_collaboration_roots [..root..]}}) => falsey
          (provided
            (auth/get-permissions ..auth.. [..root..]) => {:permissions [{:uuid        ..root..
                                                                          :permissions {:read false}}
                                                                         {:uuid        ..root2..
                                                                          :permissions {:read true}}]}))


        (fact "allows when :owner is auth user and can read all roots"
          (auth/can? ..auth.. ::auth/create {:type  "Entity"
                                             :owner ..user..
                                             :links {:_collaboration_roots [..root..]}}) => true
          (provided
            (auth/get-permissions ..auth.. [..root..]) => ..permissions..
            (auth/collect-permissions ..permissions.. :read) => [true]))

        (fact "denies when :owner is auth user and cannot read all roots"
          (auth/can? ..auth.. ::auth/create {:type  "Entity"
                                             :owner ..user..
                                             :links {:_collaboration_roots [..root..]}}) => false
          (provided
            (auth/authenticated-user-id ..auth..) => ..user..
            (auth/get-permissions ..auth.. [..root..]) => ..permissions..
            (auth/collect-permissions ..permissions.. :read) => [true false])))))

  (facts ":update"
    (against-background [(auth/authenticated-user-id ..auth..) => (str (UUID/randomUUID))]
      (fact "Delegates to get-permissions when not owner"
        (auth/can? ..auth.. ::auth/update {:type  "Entity"
                                           :owner (str (UUID/randomUUID))
                                           :links {:_collaboration_roots ..roots..}}) => true
        (provided
          (auth/get-permissions ..auth.. ..roots..) => {:permissions [{:uuid        :uuid1
                                                                       :permissions {:read  true
                                                                                     :write false
                                                                                     :admin false}}
                                                                      {:uuid        :uuid2
                                                                       :permissions {:read  true
                                                                                     :write true
                                                                                     :admin false}}]}))))
  (facts ":delete"
    (against-background [(auth/authenticated-user-id ..auth..) => ..user..]
      (fact "Annoations require :user match authenticated user"
        (auth/can? ..auth.. ::auth/delete {:type "Annotation"
                                           :user ..user..}) => true
        (auth/can? ..auth.. ::auth/delete {:type "Annotation"
                                           :user ..other..}) => false)

      (fact "allowed when entity :owner is nil"
        (auth/can? ..auth.. ::auth/delete {:type  "Entity"
                                           :owner nil}) => true
        (provided
          (auth/get-permissions ..auth.. nil) => {}))

      (fact "allowed when :write on all roots when not owner"
        (auth/can? ..auth.. ::auth/delete {:type  "Entity"
                                           :owner (str (UUID/randomUUID))
                                           :links {:_collaboration_roots ..roots..}}) => true
        (provided
          (auth/authenticated-user-id ..auth..) => (str (UUID/randomUUID))
          (auth/get-permissions ..auth.. ..roots..) => {:permissions [{:uuid        :uuid1
                                                                       :permissions {:read  true
                                                                                     :write true
                                                                                     :admin true}}
                                                                      {:uuid        :uuid2
                                                                       :permissions {:read  true
                                                                                     :write true
                                                                                     :admin false}}]}))

      (fact "Requires :write on all roots when not owner"
        (auth/can? ..auth.. ::auth/delete {:type  "Entity"
                                           :owner (str (UUID/randomUUID))
                                           :links {:_collaboration_roots ..roots..}}) => false
        (provided
          (auth/authenticated-user-id ..auth..) => (str (UUID/randomUUID))
          (auth/get-permissions ..auth.. ..roots..) => {:permissions [{:uuid        :uuid1
                                                                       :permissions {:read  true
                                                                                     :write false
                                                                                     :admin false}}
                                                                      {:uuid        :uuid2
                                                                       :permissions {:read  true
                                                                                     :write false
                                                                                     :admin true}}]})))))

(facts "About `get-permissions`"
       (fact "gets permissions from auth server"
             (let [uuids    [(str (UUID/randomUUID)) (str (UUID/randomUUID))]
                   expected {:permissions [{:uuid        (first uuids)
                                            :permissions {}}
                                           {:uuid        (last uuids)
                                            :permissions {}}]}
                   server   config/AUTH_SERVER
                   auth     {:server server}]
               (with-fake-http [{:url (util/join-path [server "api" "v2" "permissions"]) :method :get} {:body   (json/write-str expected)
                                                                                                        :status 200}]
                 (auth/get-permissions auth uuids) => expected))))


(facts "About `collect-permissions"
  (fact "gets permissions"
    (auth/collect-permissions {:permissions [{:uuid        :uuid1
                                              :permissions {:read  false
                                                            :write false
                                                            :admin true}}
                                             {:uuid        :uuid2
                                              :permissions {:read  true
                                                            :write true
                                                            :admin false}}]}
      :read) => [false true]))

