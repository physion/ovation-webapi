(ns ovation.auth
  (:require [org.httpkit.client :as http]
            [ovation.util :as util :refer [<??]]
            [ring.util.http-predicates :as hp]
            [ring.util.http-response :refer [throw!]]
            [slingshot.slingshot :refer [throw+]]
            [clojure.tools.logging :as logging]
            [clojure.data.json :as json]))



;; Authentication — this should be replaced with buddy.auth

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
              :query-params {:uuids (json/write-str (map str collaboration-roots))}
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

(defn authenticated-teams
  "Get all teams to which the authenticated user belongs"
  [auth]
  (-> @(::authenticated-teams auth)
    :body
    util/from-json
    :teams))

(defn effective-collaboration-roots
  [doc]
  (case (:type doc)
    "Project" (conj (get-in doc [:links :_collaboration_roots]) (:_id doc))

    ;; default
    (get-in doc [:links :_collaboration_roots])))

(defn- can-create?
  [auth doc]
  (let [auth-user-id (authenticated-user-id auth)]

    (case (:type doc)
      ;; User owns annotations and can read all collaboration roots
      "Annotation" (= auth-user-id (:user doc))

      "Relation" (= auth-user-id (:user_id doc))

      "Project" (or (nil? (:owner doc)) (= auth-user-id (:owner doc)))

      ;; default (Entity)
      (let [collaboration-root-ids (effective-collaboration-roots doc)
            permissions            (get-permissions auth collaboration-root-ids)]

        (and (or (nil? (:owner doc)) (= auth-user-id (:owner doc)))
          (every? true? (collect-permissions permissions :read)))))))

(defn- can-update?
  [auth doc]
  (let [auth-user-id (authenticated-user-id auth)]
    (case (:type doc)
      "Annotation" (and (= auth-user-id (:user doc)))
      "Relation" (= auth-user-id (:user_id doc))

      ;; default (Entity)
      (let [collaboration-root-ids (effective-collaboration-roots doc)
            permissions (get-permissions auth collaboration-root-ids)]
        ;; handle entity with collaboration roots
        (or
          ;; user is owner and can read all roots
          (and (= auth-user-id (:owner doc))
              (every? true? (collect-permissions permissions :read)))

          ;; user can write any of the roots
          (not (nil? (some true? (collect-permissions permissions :write)))))))))


(defn- can-delete?
  [auth doc]
  (let [auth-user-id (authenticated-user-id auth)]
    (case (:type doc)
      "Annotation" (= auth-user-id (:user doc))
      "Relation" (= auth-user-id (:user_id doc))

      ;; default
      (let [permissions (get-permissions auth (effective-collaboration-roots doc))]
        (or (every? true? (collect-permissions permissions :write))
          (= auth-user-id (:owner doc)))))))

(defn- can-read?
  [auth doc cached-teams]
  (let [authenticated-user  (authenticated-user-id auth)
        authenticated-teams (or cached-teams (authenticated-teams auth)) ;(or teams (authenticated-teams auth))
        owner               (case (:type doc)
                              "Annotation" (:user doc)
                              "Relation" (:user_id doc)
                              ;; default
                              (:owner doc))
        roots               (get-in doc [:links :_collaboration_roots])]

    ;; authenticated user is owner or is a member of a team in _collaboration_roots
    (not (nil? (or (= owner authenticated-user)
                 (some (set roots) authenticated-teams))))))


(defn can?
  [auth op doc & {:keys [teams] :or {:teams nil}}]
  (case op
    :ovation.auth/create (can-create? auth doc)
    :ovation.auth/update (can-update? auth doc)
    :ovation.auth/delete (can-delete? auth doc)
    :ovation.auth/read (can-read? auth doc teams)

    ;;default
    (throw+ {:type ::unauthorized :operation op :message "Operation not recognized"})
    ))

(defn check!
  ([auth op]
   (fn [doc]
     (when-not (can? auth op doc)
               (throw+ {:type ::unauthorized :operation op :message "Operation not authorized"}))
     doc))
  ([auth op doc]
   ((check! auth op) doc)))
