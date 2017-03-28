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
            [ovation.request-context :as rc]
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
  "Gets all teams for authenticated user as a future assoc: @{:user_uuid id :team_uuids [id1 id2]}"
  [api-token]
  (let [opts (request-opts api-token)
        url  (make-url "team_uuids")]
    (future (let [resp @(httpkit.client/get url opts)]
              (if (hp/ok? resp)
                (-> resp
                  :body
                  util/from-json))))))
(defn create-team
  [ctx team-uuid]

  (logging/info (str "Creating Team for " team-uuid))
  (let [opts (request-opts (rc/token ctx))
        url (make-url "teams")
        body (util/to-json {:team {:uuid (str team-uuid)}})
        response @(httpkit.client/post url (assoc opts :body body))]
    (when (not (http-predicates/created? response))
      (throw! response))
    (util/from-json (:body response))))


(defn- membership-result
  [team-uuid ctx response]
  (let [result (util/from-json (:body response))
        pending? (:pending_membership result)
        self-route (if pending? :put-pending-membership :put-membership)
        membership-id (or (get-in result [:pending_membership :id])
                        (get-in result [:membership :id]))]
    (if (:membership result)
      (-> result
        (dissoc :users)
        (dissoc :membership_roles)
        (assoc-in [:membership :links :self] (routes/named-route ctx self-route {:id team-uuid :mid membership-id})))
      result)))

(defn get-team*
  [ctx team-id]
  (let [opts     (request-opts (rc/token ctx))
        url      (make-url "teams" team-id)
        response @(httpkit.client/get url opts)]

    (if-let [team (cond
                    (http-predicates/ok? response) (util/from-json (:body response))

                    (http-predicates/not-found? response) (create-team ctx team-id)
                    :else (throw! (dissoc response :headers)))]

      (let [memberships        (get-in team [:team :memberships])
            linked-memberships (map #(assoc-in % [:links :self] (routes/named-route ctx :put-membership {:id team-id :mid (:id %) :org (::rc/org ctx)})) memberships)]
        (-> team
          (assoc-in [:team :type] k/TEAM-TYPE)
          (update-in [:team] dissoc :project)
          (update-in [:team] dissoc :organization)
          (update-in [:team] dissoc :project_id)
          (update-in [:team] dissoc :organization_id)
          (assoc-in [:team :memberships] linked-memberships)
          (assoc-in [:team :links] {:self        (routes/named-route ctx :get-team {:id team-id :org (::rc/org ctx)})
                                    :memberships (routes/named-route ctx :post-memberships {:id team-id :org (::rc/org ctx)})}))))))


(defn- put-membership
  [ctx team-uuid membership membership-id pending?]                              ;; membership is a TeamMembership
  (let [rt (::rc/routes ctx)
        url-path (if pending? "pending_memberships" "memberships")
        opts (request-opts (rc/token ctx))
        url (make-url url-path membership-id)
        role-id (get-in membership [:role :id])
        body {:membership {:role_id role-id}}]
    (when (or (nil? role-id)
            (nil? membership-id))
      (unprocessable-entity!))

    (let [response (dissoc @(httpkit.client/put url (assoc opts :body (util/to-json body))) :headers)]
      (when (not (http-predicates/ok? response))
        (throw! response))

      (membership-result team-uuid ctx response))))

(defn put-membership*
  [ctx team-uuid membership membership-id]                              ;; membership is a TeamMembership
  (put-membership ctx team-uuid membership membership-id false))

(defn put-pending-membership*
  [ctx team-uuid membership membership-id]                              ;; membership is a PendingTeamMembership
  (put-membership ctx team-uuid membership membership-id true))


(defn post-membership*
  [ctx team-uuid membership]                            ;; membership is a NewTeamMembership
  (let [rt      (::rc/routes ctx)
        opts    (request-opts (rc/token ctx))
        url     (make-url "memberships")
        team    (get-team* ctx team-uuid)
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
        (throw! (dissoc response :headers)))

      (membership-result team-uuid ctx response))))

(defn delete-membership
  [ctx membership-id pending?]
  (let [url-path (if pending? "pending_memberships" "memberships")
        opts (request-opts (rc/token ctx))
        url (make-url url-path membership-id)]

    (let [response (dissoc @(httpkit.client/delete url opts) :headers)]
      (when (not (http-predicates/no-content? response))
        (throw! response)))))

(defn delete-membership*
  [ctx membership-id]
  (delete-membership ctx membership-id false))


(defn delete-pending-membership*
  [ctx membership-id]
  (delete-membership ctx membership-id true))

(defn get-roles*
  [ctx]
  (let [opts (request-opts (rc/token ctx))
        url (make-url "roles")]

    (let [response @(httpkit.client/get url opts)]
      (when (not (http-predicates/ok? response))
        (throw! response))
      (let [body (util/from-json (:body response))
            roles (:roles body)]
        {:roles roles}))))
