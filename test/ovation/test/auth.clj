(ns ovation.test.auth
  (:use midje.sweet)
  (:require [ovation.auth :as auth]
            [org.httpkit.fake :refer [with-fake-http]]
            [clojure.walk :as walk]
            [clojure.data.json :as json]
            [clojure.data.codec.base64 :as b64]
            [ovation.util :as util])
  (:import (clojure.lang ExceptionInfo)))


(defn string-to-base64-string [original]
  (String. (b64/encode (.getBytes original)) "UTF-8"))

(facts "About user authorization"
  (facts "with valid api key"
    (fact "gets auth info"
      (let [apikey "my-api-key"
            server "https://some.ovation.io"
            auth (clojure.string/join ":" [apikey apikey])
            b64auth (string-to-base64-string auth)
            ckey "cloudant-api-key"
            curl "<cloudant db url>"
            session (util/to-json {:cloudant_key    ckey
                                   :cloudant_db_url curl})]
        (with-fake-http [(clojure.string/join "/" [server "api" "v1" "users"]) session] ;{:url (clojure.string/join "/" [server "api" "v1" "users"]) :headers {"authorization" b64auth}}

          (util/from-json (:body @(auth/get-auth server apikey)))) => {:cloudant_key    ckey
                                                                       :cloudant_db_url curl}))

    (fact "auth-map returns body"
      (let [apikey "my-api-key"
            server "https://some.ovation.io"
            auth (clojure.string/join ":" [apikey apikey])
            b64auth (string-to-base64-string auth)
            ckey "cloudant-api-key"
            curl "<cloudant db url>"
            session (util/to-json {:cloudant_key    ckey
                                   :cloudant_db_url curl})]
        (with-fake-http [(clojure.string/join "/" [server "api" "v1" "users"]) {:body   session
                                                                                :status 200}] ;{:url (clojure.string/join "/" [server "api" "v1" "users"]) :headers {"authorization" b64auth}}

          (auth/auth-info (auth/get-auth server apikey))) => {:cloudant_key    ckey
                                                              :cloudant_db_url curl}))

    (fact "authorize returns authorization info"
      (let [apikey "my-api-key"
            server "https://some.ovation.io"
            auth (clojure.string/join ":" [apikey apikey])
            b64auth (string-to-base64-string auth)
            ckey "cloudant-api-key"
            curl "<cloudant db url>"
            session (util/to-json {:cloudant_key    ckey
                                   :cloudant_db_url curl})]
        (with-fake-http [(clojure.string/join "/" [server "api" "v1" "users"]) {:body   session
                                                                                :status 200}] ;{:url (clojure.string/join "/" [server "api" "v1" "users"]) :headers {"authorization" b64auth}}

          (auth/authorize server apikey) => {:cloudant_key    ckey
                                             :cloudant_db_url curl}))

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
