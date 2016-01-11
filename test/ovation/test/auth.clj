(ns ovation.test.auth
  (:use midje.sweet)
  (:require [ovation.auth :as auth]
            [org.httpkit.fake :refer [with-fake-http]]
            [clojure.data.codec.base64 :as b64]
            [ovation.util :as util]
            [clojure.data.json :as json])
  (:import (clojure.lang ExceptionInfo)
           (java.util UUID)))


(defn string-to-base64-string [original]
  (String. (b64/encode (.getBytes original)) "UTF-8"))

(facts "About user authorization"
  (facts "with valid api key"
    (fact "gets auth info"
      (let [apikey  "my-api-key"
            server  "https://some.ovation.io"
            auth    (clojure.string/join ":" [apikey apikey])
            ckey    "cloudant-api-key"
            curl    "<cloudant db url>"
            session (util/to-json {:cloudant_key    ckey
                                   :cloudant_db_url curl})]
        (with-fake-http [(clojure.string/join "/" [server "api" "v1" "users"]) session] ;{:url (clojure.string/join "/" [server "api" "v1" "users"]) :headers {"authorization" b64auth}}

          (util/from-json (:body @(auth/get-auth server apikey)))) => {:cloudant_key    ckey
                                                                       :cloudant_db_url curl}))

    (fact "auth-map returns body"
      (let [apikey  "my-api-key"
            server  "https://some.ovation.io"
            auth    (clojure.string/join ":" [apikey apikey])
            b64auth (string-to-base64-string auth)
            ckey    "cloudant-api-key"
            curl    "<cloudant db url>"
            session (util/to-json {:cloudant_key    ckey
                                   :cloudant_db_url curl})]
        (with-fake-http [(clojure.string/join "/" [server "api" "v1" "users"]) {:body   session
                                                                                :status 200}] ;{:url (clojure.string/join "/" [server "api" "v1" "users"]) :headers {"authorization" b64auth}}

          (auth/auth-info (auth/get-auth server apikey))) => {:cloudant_key    ckey
                                                              :cloudant_db_url curl}))

    (fact "authorize returns authorization info"
      (let [apikey  "my-api-key"
            server  "https://some.ovation.io"
            auth    (clojure.string/join ":" [apikey apikey])
            b64auth (string-to-base64-string auth)
            ckey    "cloudant-api-key"
            curl    "<cloudant db url>"
            session (util/to-json {:cloudant_key    ckey
                                   :cloudant_db_url curl})]
        (with-fake-http [(clojure.string/join "/" [server "api" "v1" "users"]) {:body   session
                                                                                :status 200}] ;{:url (clojure.string/join "/" [server "api" "v1" "users"]) :headers {"authorization" b64auth}}

          (auth/authenticate server apikey) => {:cloudant_key    ckey
                                                :cloudant_db_url curl
                                                :server          server}))

      ))

  (facts "without valid api key"
    (fact "check-auth +throws 401"
      (let [apikey "my-api-key"
            server "https://some.ovation.io"]
        (with-fake-http [(clojure.string/join "/" [server "api" "v1" "users"]) {:status 401}] ;{:url (clojure.string/join "/" [server "api" "v1" "users"]) :headers {"authorization" b64auth}}

          (auth/check-auth (auth/get-auth server apikey)) => (throws ExceptionInfo)))
      )

    (fact "auth-map +throws 401"
      (let [apikey "my-api-key"
            server "https://some.ovation.io"]
        (with-fake-http [(clojure.string/join "/" [server "api" "v1" "users"]) {:status 401}] ;{:url (clojure.string/join "/" [server "api" "v1" "users"]) :headers {"authorization" b64auth}}

          (auth/auth-info (auth/get-auth server apikey)) => (throws ExceptionInfo))))

    )

  )

(facts "About authorized user"
  (fact "`authorized-user-id` returns user UUID"
    (auth/authenticated-user-id ...auth...) => ...id...
    (provided
      ...auth... =contains=> {:uuid ...id...})))

(facts "About `can?`"
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
             (let [uuids [(str (UUID/randomUUID)) (str (UUID/randomUUID))]
                   expected {:permissions [{:uuid        (first uuids)
                                            :permissions {}}
                                           {:uuid        (last uuids)
                                            :permissions {}}]}
                   server "https://some.ovation.io"
                   auth {:server server}]
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

