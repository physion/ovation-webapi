(ns ovation.middleware.auth
  "HTTP token authorization and authenticated user info middleware for ovation.io"
  (:require [ovation.auth :as auth]
            [clojure.string :refer [lower-case]]
            [ovation.teams :as teams]
            [ovation.auth :as auth]
            [ovation.logging :as logging]))

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

(defn wrap-log-identity
  [handler]
  (fn [request]
    (if-let [user (get-in request [:identity :uuid])]
      (do
         (logging/info (format "[AUDIT] User %s" user))
         (handler request)))))
