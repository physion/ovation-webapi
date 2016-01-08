(ns ovation.auth
  (:require [org.httpkit.client :as http]
            [ovation.util :as util]
            [ring.util.http-predicates :as hp]
            [ring.util.http-response :refer [throw!]]
            [slingshot.slingshot :refer [throw+]]
            [clojure.tools.logging :as logging]
            [clojure.data.json :as json]))



;; Authentication

(defn get-auth
  "Async get user info for ovation.io API key"
  [authserver apikey]
  (let [url (util/join-path [authserver "api" "v1" "users"])
        opts {:basic-auth [apikey apikey]
              :accept     :json}]

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

(defn authenticate
  "Gets the Cloudant API key and database URL for an Ovation API key."
  [authserver apikey]
  (-> (auth-info (get-auth authserver apikey))
      (assoc :server authserver)))

(defn authenticated-user-id
  "The UUID of the authorized user"
  [auth]
  (:uuid auth))


;; Authorization
(defn get-permissions
  [auth collaboration-roots]
  (let [url (util/join-path [(:server auth) "api" "v2" "permissions"])
        apikey (:api_key auth)
        opts {:basic-auth   [apikey apikey]
              :query-params {:uuids (json/write-str collaboration-roots)}
              :accept       :json}]

    (let [response @(http/get url opts)]
      (when (not (hp/ok? response))
        (logging/error "Unable to retrieve object permissions")
        (throw! response))

      (-> response
          :body
        (util/from-json)))))


(defn collect-permissions
  [permissions perm]
  (map #(-> % :permissions perm) (:permissions permissions)))

(defn- can-write?
  [auth doc]
  (let [auth-user-id (authenticated-user-id auth)]
    (case (:type doc)
      "Annotation" (= auth-user-id (:user doc))
      ;; default
      (let [collaboration-root-ids (get-in doc [:links :_collaboration_roots])
            permissions (get-permissions auth collaboration-root-ids)]
        (or
          ;; user is owner and can read all roots
          (and (or (nil? (:owner doc)) (= auth-user-id (:owner doc)))
              (every? true? (collect-permissions permissions :read)))

          ;; user can write any of the roots
          (some true? (collect-permissions permissions :write)))))))


(defn- can-delete?
  [auth doc]
  (let [auth-user-id (authenticated-user-id auth)]
    (case (:type doc)
      "Annotation" (= auth-user-id (:user doc))
      ;; default
      (let [permissions (get-permissions auth (get-in doc [:links :_collaboration_roots]))]
        (or (every? true? (collect-permissions permissions :write))
          (= auth-user-id (:owner doc)))))))

(defn can?
  [auth op doc]
  (case op
    ::create (can-write? auth doc)
    ::update (can-write? auth doc)
    ::delete (can-delete? auth doc)
    ;;default
    (not (nil? (authenticated-user-id auth)))))

(defn check!
  ([auth op]
   (fn [doc]
     (when-not (can? auth op doc)
               (throw+ {:type ::unauthorized :operation op :message "Operation not authorized"}))
     doc))
  ([auth op doc]
   ((check! auth op) doc)))
