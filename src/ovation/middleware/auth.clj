(ns ovation.middleware.auth
  "HTTP token authorization and authenticated user info middleware for ovation.io"
  (:require [ovation.auth :as auth]
            [clojure.string :refer [lower-case]]
            [ovation.teams :as teams]
            [ovation.auth :as auth]
            [clojure.string :as string]))

(defn service-sub?
  [identity]
  "Returns true if :sub contains `@clients`"
  (if-let [sub (:sub identity)]
    (boolean (re-matches #"^.*@clients$" sub))
    false))

(defn scopes
  [identity]
  "Returns an array of scopes for identity"
  (if-let [scope-string (or (:scope identity) ((keyword "https://ovation.io/scope") identity))]
    (-> scope-string
      (string/split #" "))
    []))

(defn wrap-auth
  "Wrap the response with a future authenticated-teams andscopes"
  [handler]
  (fn [request]
    (if (auth/authenticated? request)
      (-> request
        (assoc-in [:identity ::auth/authenticated-teams] (teams/get-teams (auth/token request)))
        (assoc-in [:identity ::auth/authenticated-permissions] (auth/get-permissions (auth/token request)))
        (assoc-in [:identity ::auth/service-account] (service-sub? (:identity request)))
        (assoc-in [:identity ::auth/scopes] (scopes (:identity request)))
        (handler))
      (handler request))))
