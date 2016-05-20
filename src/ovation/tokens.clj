(ns ovation.tokens
  (:require [org.httpkit.client :as http]
            [ovation.config :as config]
            [ovation.util :as util]
            [clojure.data.json :as json]
            [clojure.walk :as walk]
            [ovation.auth :as auth]))

(def auth-service-url (util/join-path [config/AUTH_SERVER "api" "v1" "sessions"]))


(defn post-json
  [url body]
  (let [body     (util/write-json-body body)
        options  {:body    body
                  :headers {"Content-Type" "application/json"}}


        response (http/post url options)]

       (-> @response
         (dissoc :opts)
         (dissoc :headers)
         (assoc :status (int (:status @response)))
         (assoc :body (walk/keywordize-keys (json/read-str (:body @response)))))))





(defn get-token
  "Gets a new authorization token from the AUTH_SERVER, returning the full HTTP response"
  [email password]

  (post-json auth-service-url {:email email :password password}))


(defn refresh-token
  [request]
  (let [token (-> request
                (auth/identity)
                (::auth/token))]

    (post-json (util/join-path [auth-service-url "refresh"]) {:token token})))


