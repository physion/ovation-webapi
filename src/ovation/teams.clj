(ns ovation.teams
  (:require [ovation.routes :as routes]
            [ovation.util :as util]
            [ovation.config :as config]
            [org.httpkit.client :as httpkit.client]
            [clojure.tools.logging :as logging]
            [ring.util.http-predicates :as http-predicates]
            [ring.util.http-response :refer [throw! bad-request! not-found!]]
            [ovation.core :as core]
            [ovation.auth :as auth]))


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
  [request team-id]
  (let [rt (routes/router request)
        opts (-request-opts (api-key request))
        url (routes/named-route rt :post-teams {})
        body (util/to-json {:team {}})
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

      (assoc-in team [:team :links] {:self        (routes/named-route rt :get-team {:id team-id})
                                     :memberships (routes/named-route rt :post-memberships {:id team-id})}))))



(defn put-membership*
  [request team-id membership])

(defn post-membership* {}
  [request team-id membership]
  (let [rt (routes/router request)
        opts (-request-opts (api-key request))
        url (-make-url "teams" team-id "memberships")
        team (or (get-team* request team-id :allow-nil false)
               (create-team request team-id))
        body (-> membership
               (assoc :team_uuid (:uuid (:uuid team))))]

    (let [response @(httpkit.client/post url (assoc opts :body (util/to-json body)))]
      (when (not (http-predicates/created? response))
        (throw! response))

      (let [result (util/from-json (:body response))
            membership-id (get-in result [:membership :id])]
        (-> result
          (assoc-in [:membership :links :self] (routes/named-route rt :put-membership {:id team-id :mid membership-id})))))))
