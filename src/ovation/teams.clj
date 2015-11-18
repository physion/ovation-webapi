(ns ovation.teams
  (:require [ovation.routes :as routes]
            [ovation.util :as util]
            [ovation.config :as config]
            [org.httpkit.client :as httpkit.client]
            [clojure.tools.logging :as logging]
            [ring.util.http-predicates :as http-predicates]
            [ring.util.http-response :refer [throw! bad-request! not-found! unprocessable-entity!]]
            [ovation.core :as core]
            [ovation.auth :as auth]
            [ovation.constants :as k]))


(defn api-key
  [request]
  (let [auth (:auth/auth-info request)]
    (:api_key auth)))

(defn -make-url
  [& comps]
  (util/join-path (conj comps config/TEAMS_SERVER)))

(defn -request-opts
  [api-key]
  {:timeout    200                                          ; ms
   :basic-auth [api-key "X"]
   :headers    {"Content-Type" "application/json; charset=utf-8"}})

(defn -default-team
  [request team-id]
  (let [auth (:auth/auth-info request)
        rt (routes/router request)
        project (first (core/get-entities auth [team-id] rt))]

    (when (nil? project)
      (not-found! {:errors {:detail "Project not found"}}))
    (if-let [owner (core/get-owner auth rt project)]
      {:id          team-id
       :memberships [{:id    "?"
                      :email (get-in owner [:attributes :email])}]})))

(defn create-team
  [request team-uuid]
  (let [rt (routes/router request)
        opts (-request-opts (api-key request))
        url (routes/named-route rt :post-teams {})
        body (util/to-json {:team {:uuid team-uuid}})
        response @(httpkit.client/post url (assoc opts :body body))]
    (when (not (http-predicates/created? response))
      (throw! response))
    (util/from-json (:body response))))

(defn get-team*
  [request team-id & {:keys [allow-nil]
                      :or   {allow-nil false}}]
  (let [rt (routes/router request)
        opts (-request-opts (api-key request))
        url (-make-url "teams" team-id)
        response @(httpkit.client/get url opts)]

    (if-let [team (cond
                    (http-predicates/ok? response) (util/from-json (:body response))

                    (http-predicates/not-found? response) (when (not allow-nil)
                                                            (not-found! {:errors {:detail "Team not found"}}))
                    :else (throw! response))]

      (-> team
        ;(assoc-in [:team :id] (:uuid team))
        ;(update-in [:team] dissoc :uuid)
        (assoc-in [:team :type] k/TEAM-TYPE)
        (update-in [:team] dissoc :project)
        (update-in [:team] dissoc :organization)
        (assoc-in [:team :links] {:self        (routes/named-route rt :get-team {:id team-id})
                                  :memberships (routes/named-route rt :post-memberships {:id team-id})})))))



;{:id    s/Int,
; :team_id s/Int,
; :added s/Str
; :role_id s/Int,
; :user {:id s/Int
;        :uuid s/Uuid
;        :name s/Str
;        :email s/Str
;        :links {s/Keyword s/Str}}
; :links {s/Keyword s/Str}}

(defn -membership-result
  [team-uuid rt response]
  (let [result (util/from-json (:body response))
        membership-id (get-in result [:membership :id])]
    (-> result
      (assoc-in [:membership :links :self] (routes/named-route rt :put-membership {:id team-uuid :mid membership-id})))))

(defn put-membership*
  [request team-uuid membership]                              ;; membership is a TeamMembership
  (let [rt (routes/router request)
        opts (-request-opts (api-key request))
        url (-make-url "teams" team-uuid "memberships" (:id membership))
        role-id (get-in membership [:role :id])
        body {:membership {:role_id role-id}}]
    (when (nil? role-id)
      (throw! unprocessable-entity!))

    (let [response @(httpkit.client/put url (assoc opts :body (util/to-json body)))]
      (when (not (http-predicates/ok? response))
        (throw! response))

      (-membership-result team-uuid rt response))))


(defn post-membership*
  [request team-uuid membership]                            ;; membership is a NewTeamMembership
  (let [rt (routes/router request)
        opts (-request-opts (api-key request))
        url (-make-url "teams" team-uuid "memberships")
        team (or (get-team* request team-uuid :allow-nil true)
               (do
                 (logging/info (str "Creating Team for " team-uuid))
                 (create-team request team-uuid)))
        team-id (get-in team [:team :id])
        role (:role membership)
        email (:email membership)
        body {:membership {:team_id team-id
                           :role_id (:id role)
                           :email   email}}]

    (when (or (nil? team-id) (nil? role) (nil? email))
      (throw! unprocessable-entity!))

    (let [response @(httpkit.client/post url (assoc opts :body (util/to-json body)))]
      (when (not (http-predicates/created? response))
        (throw! response))

      (-membership-result team-uuid rt response))))


(defn delete-membership*
  [request team-uuid membership-id]
  (let [rt (routes/router request)
        opts (-request-opts (api-key request))
        url (-make-url "teams" team-uuid "memberships" membership-id)]

    (let [response @(httpkit.client/delete url opts)]
      (when (not (http-predicates/ok? response))
        (throw! response))

      (-membership-result team-uuid rt response))))

(defn get-roles*
  [request]
  (let [opts (-request-opts (api-key request))
        url (-make-url "roles")]

    (let [response @(httpkit.client/get url opts)]
      (when (not (http-predicates/ok? response))
        (throw! response))
      (let [body (util/from-json (:body response))
            roles (:roles body)]
        {:roles roles}))))
