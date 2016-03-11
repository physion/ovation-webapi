(ns ovation.teams
  (:require [ovation.routes :as routes]
            [ovation.util :as util]
            [ovation.config :as config]
            [org.httpkit.client :as httpkit.client]
            [clojure.tools.logging :as logging]
            [ring.util.http-predicates :as http-predicates]
            [ring.util.http-response :refer [throw! bad-request! not-found! unprocessable-entity!]]
            [ovation.auth :as auth]
            [ovation.constants :as k]
            [clojure.core.async :refer [chan >!!]]
            [slingshot.support :refer [get-throwable]]
            [ring.util.http-predicates :as hp]))


(defn auth-token
  [request]
  (auth/token request))

(defn make-url
  [& comps]
  (util/join-path (conj comps config/TEAMS_SERVER)))

(defn request-opts
  [token]
  {:timeout     10000                                          ; ms
   :oauth-token token
   :headers     {"Content-Type" "application/json; charset=utf-8"}})

(defn get-teams
  "Gets all teams for authenticated user as a future: [id1 id2]"
  [api-token]
  (let [opts (request-opts api-token)
        url  (make-url "team_uuids")]
    (future (let [resp @(httpkit.client/get url opts)]
              (if (hp/ok? resp)
                (-> resp
                  :body
                  util/from-json
                  :team_uuids))))))

(defn create-team
  [request team-uuid]

  (logging/info (str "Creating Team for " team-uuid))
  (let [opts (request-opts (auth-token request))
        url (make-url "teams")
        body (util/to-json {:team {:uuid (str team-uuid)}})
        response @(httpkit.client/post url (assoc opts :body body))]
    (when (not (http-predicates/created? response))
      (throw! response))
    (util/from-json (:body response))))


(defn -membership-result
  [team-uuid rt response]
  (let [result (util/from-json (:body response))
        membership-id (or (get-in result [:pending_membership :id])
                        (get-in result [:membership :id]))]
    (if (:membership result)
      (-> result
        (dissoc :users)
        (dissoc :membership_roles)
        (assoc-in [:membership :links :self] (routes/named-route rt :put-membership {:id team-uuid :mid membership-id})))
      result)))

(defn get-team*
  [request team-id]
  (let [rt (routes/router request)
        opts (request-opts (auth-token request))
        url (make-url "teams" team-id)
        response @(httpkit.client/get url opts)]

    (if-let [team (cond
                    (http-predicates/ok? response) (util/from-json (:body response))

                    (http-predicates/not-found? response) (create-team request team-id)
                    :else (throw! response))]

      (let [memberships (get-in team [:team :memberships])
            linked-memberships (map #(assoc-in % [:links :self] (routes/named-route rt :put-membership {:id team-id :mid (:id %)})) memberships)]
        (-> team
          (assoc-in [:team :type] k/TEAM-TYPE)
          (update-in [:team] dissoc :project)
          (update-in [:team] dissoc :organization)
          (update-in [:team] dissoc :project_id)
          (update-in [:team] dissoc :organization_id)
          (assoc-in [:team :memberships] linked-memberships)
          (assoc-in [:team :links] {:self        (routes/named-route rt :get-team {:id team-id})
                                    :memberships (routes/named-route rt :post-memberships {:id team-id})}))))))


(defn put-membership*
  [request team-uuid membership membership-id]                              ;; membership is a TeamMembership
  (let [rt (routes/router request)
        opts (request-opts (auth-token request))
        url (make-url "memberships" membership-id)
        role-id (get-in membership [:role :id])
        body {:membership {:role_id role-id}}]
    (when (or (nil? role-id)
            (nil? membership-id))
      (unprocessable-entity!))

    (let [response @(httpkit.client/put url (assoc opts :body (util/to-json body)))]
      (when (not (http-predicates/ok? response))
        (throw! response))

      (-membership-result team-uuid rt response))))


(defn post-membership*
  [request team-uuid membership]                            ;; membership is a NewTeamMembership
  (let [rt      (routes/router request)
        opts    (request-opts (auth-token request))
        url     (make-url "memberships")
        team    (get-team* request team-uuid)
        team-id (get-in team [:team :id])
        role    (:role membership)
        email   (:email membership)
        body    {:membership {:team_id team-uuid
                           :role_id (:id role)
                           :email   email}}]

    (when (or (nil? team-id) (nil? role) (nil? email))
      (unprocessable-entity!))

    (let [response @(httpkit.client/post url (assoc opts :body (util/to-json body)))]
      (when (not (http-predicates/created? response))
        (throw! response))

      (-membership-result team-uuid rt response))))


(defn delete-membership*
  [request membership-id]
  (let [opts (request-opts (auth-token request))
        url (make-url "memberships" membership-id)]

    (let [response @(httpkit.client/delete url opts)]
      (when (not (http-predicates/no-content? response))
        (throw! response)))))

(defn get-roles*
  [request]
  (let [opts (request-opts (auth-token request))
        url (make-url "roles")]

    (let [response @(httpkit.client/get url opts)]
      (when (not (http-predicates/ok? response))
        (throw! response))
      (let [body (util/from-json (:body response))
            roles (:roles body)]
        {:roles roles}))))
