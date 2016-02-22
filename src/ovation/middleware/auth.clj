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
        (assoc-in [:identity ::auth/authenticated-teams] (teams/teams (auth/token request)))
        (handler))
      (handler request))))


(defn wrap-jwt
  "Wrap the response with JWT token authentication"
  [handler & {:keys [wraper required-auth-url-prefix]}]
  (fn [request]
    (if (and required-auth-url-prefix
          (not (empty? (filter #(.startsWith (lower-case (:uri request)) (lower-case %)) required-auth-url-prefix))))
      (wraper request)
      (handler request))))
