(ns ovation.auth
  (:require [org.httpkit.client :as http]
            [ovation.util :as util]
            [ring.util.http-predicates :as hp]
            [ring.util.http-response :refer [throw!]]))


(defn get-auth
  "Async get user info for ovation.io API key"
  [authserver apikey]
  (let [url (clojure.string/join "/" [authserver "api" "v1" "users"])
        opts {:basic-auth [apikey apikey]}]

    (http/get url opts)))

(defn check-auth
  "Slinghsots authorization failure if not status OK."
  [auth]

  (when (not (hp/ok? auth))
    (throw! auth))

  auth)

(defn auth-info
  "Converts authorize result to map"
  [auth]

  (let [response @auth]
    (-> response
        (check-auth)
        (:body)
        (util/from-json))))

(defn authorize
  "Gets the Cloudant API key and database URL for an Ovation API key."
  [authserver apikey]
  (auth-info (get-auth authserver apikey)))

(defn authorized-user-id
  "The UUID of the authorized user"
  [auth]
  (:uuid auth))
