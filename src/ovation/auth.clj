(ns ovation.auth
  (:refer-clojure :exclude [identity])
  (:require [org.httpkit.client :as http]
            [ovation.util :as util :refer [<??]]
            [ring.util.http-predicates :as hp]
            [ring.util.http-response :refer [throw! unauthorized! forbidden! unauthorized forbidden]]
            [slingshot.slingshot :refer [throw+]]
            [clojure.tools.logging :as logging]
            [buddy.auth]
            [ovation.config :as config]
            [com.climate.newrelic.trace :refer [defn-traced]]))



(defn authenticated?
  [request]
  (buddy.auth/authenticated? request))

(defn token
  [request]
  (if-let [auth (get-in request [:headers "authorization"])]
    (last (re-find #"^Bearer (.*)$" auth))))

(defn make-bearer
  "Generates a Bearer token for the authenticated identity"
  [auth]
  (str "Bearer " (::token auth)))

(defn identity
  "Gets the authenticated identity for request. Assoc's bearer token as ::token "
  [request]
  (let [id (:identity request)]
    (if (map? id)
      (assoc id ::token (token request))
      id)))

(defn unauthorized-response
  "A default response constructor for an unathorized request."
  [request value]
  (if (authenticated? request)
    (forbidden "Permission denied")
    (unauthorized "Unauthorized")))


(defn authenticated-user-id
  "The UUID of the authorized user"
  [auth]
  (if-let [ateams (::authenticated-teams auth)]
    (:user_uuid (deref ateams 500 {:user_uuid nil}))))


;; Authorization
(defn-traced permissions
  [token]
  (let [url (util/join-path [config/AUTH_SERVER "api" "v2" "permissions"])
        opts {:oauth-token   token
              :accept       :json}]

    (future (let [response @(http/get url opts)]
              (when (not (hp/ok? response))
                (logging/error "Unable to retrieve object permissions")
                (throw! response))

              (-> response
                :body
                (util/from-json)
                :permissions)))))


(defn-traced get-permissions
  [auth collaboration-roots]
  (let [permissions (deref (::authenticated-permissions auth) 500 [])
        root-set (set collaboration-roots)]
    (filter #(contains? root-set (:uuid %)) permissions)))  ;;TODO we should collect once and then select-keys


(defn-traced collect-permissions
  [permissions perm]
  (map #(-> % :permissions perm) permissions))

(defn-traced authenticated-teams
  "Get all teams to which the authenticated user belongs or nil on failure or non-JSON response"
  [auth]
  (if-let [ateams (::authenticated-teams auth)]
    (:team_uuids (deref ateams 500 {:team_uuids []}))))

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
      "Annotation" (= auth-user-id (:user doc))
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
      "Relation" (or (= auth-user-id (:user_id doc))
                   (let [roots (get-in doc [:links :_collaboration_roots])
                         permissions (get-permissions auth roots)]
                     (every? true? (collect-permissions permissions :write))))

      ;; default
      (let [permissions (get-permissions auth (effective-collaboration-roots doc))]
        (or (every? true? (collect-permissions permissions :write))
          (= auth-user-id (:owner doc)))))))

(defn- can-read?
  [auth doc & {:keys [cached-teams] :or [cached-teams nil]}]
  (let [authenticated-user  (authenticated-user-id auth)
        authenticated-teams (or cached-teams (authenticated-teams auth))
        owner               (case (:type doc)
                              "Annotation" (:user doc)
                              "Relation" (:user_id doc)
                              ;; default
                              (:owner doc))
        roots               (effective-collaboration-roots doc)]

    ;; authenticated user is owner or is a member of a team in _collaboration_roots
    (not (nil? (or (= owner authenticated-user)
                 (some (set roots) authenticated-teams))))))


(defn-traced can?
  [auth op doc & {:keys [teams] :or {:teams nil}}]
  (case op
    ::create (can-create? auth doc)
    ::update (can-update? auth doc)
    ::delete (can-delete? auth doc)
    ::read (can-read? auth doc :teams teams)

    ;;default
    (throw+ {:type ::unauthorized :operation op :message "Operation not recognized"})))


(defn-traced check!
  ([auth op]
   (fn [doc]
     (when-not (can? auth op doc)
               (throw+ {:type ::unauthorized :operation op :message "Operation not authorized"}))
     doc))
  ([auth op doc]
   ((check! auth op) doc)))
