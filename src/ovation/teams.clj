(ns ovation.teams
  (:require [ovation.routes :as routes]))


(defn get-team*
  [request team-id]
  (let [rt (routes/router request)
        auth (:auth/auth-info request)
        api-key (:api_key auth)]

    {:team {:id          team-id
            :memberships []
            :links       {:self (routes/named-route rt :get-team {:id team-id})}}}))
