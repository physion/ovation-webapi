(ns ovation.teams
  (:require [ovation.routes :as routes]
            [ovation.util :as util]
            [ovation.config :as config]
            [org.httpkit.client :as httpkit.client]
            [clojure.tools.logging :as logging]
            [ring.util.http-predicates :as http-predicates]
            [ring.util.http-response :refer [throw! bad-request!]]))


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
   :basic-auth [api-key "X"]})

(defn get-team*
  [request team-id]
  (let [rt (routes/router request)
        opts (-request-opts (api-key request))
        url (-make-url "teams" team-id)
        response @(httpkit.client/get url opts)]

    (when (not (http-predicates/ok? response))
      (throw! response))

    (let [team (util/from-json (:body response))]
      (assoc-in team [:team :links] {:self  (routes/named-route rt :get-team {:id team-id})
                                     :roles (routes/named-route rt :all-roles {:id team-id})}))))

(defn get-memberships*
  [request team-id]
  (let [opts (-request-opts (api-key request))
        url (-make-url "teams" team-id "memberships")
        response @(httpkit.client/get url opts)]
    (when (not (http-predicates/ok? response))
      (throw! response))

    (let [memberships (util/from-json (:body response))]
      memberships)))

(defn post-membership*
  [request team-id new-membership]
  (let [rt (routes/router request)
        api-key (api-key request)]
    {:membership {}}))

(defn put-membership*
  [request team-id membership])

(defn post-membership*
  [request team-id membership]
  (let [rt (routes/router request)
        api-key (api-key request)]
    {:membership {}}))

(defn get-roles*
  [request]
  (let [rt (routes/router request)
        api-key (api-key request)]
    {:roles []}))
