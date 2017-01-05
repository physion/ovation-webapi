(ns ovation.middleware.auth
  "HTTP token authorization and authenticated user info middleware for ovation.io"
  (:require [ovation.auth :as auth]
            [clojure.string :refer [lower-case]]
            [ovation.teams :as teams]
            [ovation.auth :as auth]))

(defn wrap-authenticated-teams
  "Wrap the response with a future authenticated-teams"
  [handler]
  (fn [request]
    (if (auth/authenticated? request)
      (-> request
        (assoc-in [:identity ::auth/authenticated-teams] (teams/get-teams (auth/token request)))
        (assoc-in [:identity ::auth/authenticated-permissions] (auth/permissions (auth/token request)))
        (handler))
      (handler request))))
