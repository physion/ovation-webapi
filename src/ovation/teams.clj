(ns ovation.teams
  (:require [ovation.routes :as routes]))


(defn api-key
  [request]
  (let [auth (:auth/auth-info request)]
    (:api_key auth)))

(defn get-team*
  [request team-id]
  (let [rt (routes/router request)
        api-key (api-key request)]

    {:team {:id          team-id
            :memberships []
            :links       {:self (routes/named-route rt :get-team {:id team-id})}}}))

(defn post-membership*
  [request team-id new-membership]
  (let [rt (routes/router request)
        api-key (api-key request)]
    {:membership {}}))

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
