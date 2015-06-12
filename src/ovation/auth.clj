(ns ovation.auth
  (:require [org.httpkit.client :as http]
            [ovation.util :as util]
            [ring.util.http-predicates :as hp]
            [ring.util.http-response :refer [throw!]]
            [slingshot.slingshot :refer [throw+]]
            [clojure.tools.logging :as logging]))


(defn get-auth
  "Async get user info for ovation.io API key"
  [authserver apikey]
  (let [url (util/join-path [authserver "api" "v1" "users"])
        opts {:basic-auth [apikey apikey]}]

    (http/get url opts)))

(defn check-auth
  "Slinghsots authorization failure if not status OK."
  [auth]

  (when (not (hp/ok? auth))
    (logging/info "Authentication failed")
    (throw! auth))

  (logging/info "Authentication succeeded")
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

(defn authenticated-user-id
  "The UUID of the authorized user"
  [auth]
  (:uuid auth))

(defn- can-write?
  [auth-user-id doc]
  (or (nil? (:owner doc)) (= auth-user-id (:owner doc))))

(defn can?
  [auth-user-id op doc]
  (case op
    ::update (can-write? auth-user-id doc)
    ::delete (can-write? auth-user-id doc)
    ;;default
    (not (nil? auth-user-id))))

(defn check!
  ([auth-user-id op]
   (fn [doc]
     (when-not (can? auth-user-id op doc)
               (throw+ {:type ::unauthorized :operation op :message "Operation not authorized"}))
     doc))
  ([auth-user-id op doc]
   ((check! auth-user-id op) doc)))
