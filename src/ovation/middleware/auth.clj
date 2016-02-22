(ns ovation.middleware.auth
  "HTTP token authorization and authenticated user info middleware for ovation.io"
  (:require [ovation.auth :as auth]
            [clojure.string :refer [lower-case]]
            [ovation.teams :as teams]
            [ovation.auth :as auth]))

;; Original code from https://github.com/jstewart/ring-token-authentication/blob/master/src/ring/middleware/token_authentication.clj
;; Used under the EPL 1.0 license

;(defn token-auth-request
;  "Authenticates the given request against using a fn (f) that accepts a single
;  string parameter that is the token parsed from the authentication header.
;  The return value is added to the request with the keyword :token-authentication.
;  true indicates successful auth, while false or nil indicates failure.
;  failure."
;  [request authserver]
;  (let [auth ((:headers request) "authorization")
;        token (and auth (last (re-find #"Bearer (.*)$" auth)))
;        auth (auth/authenticate authserver token)]             ;; throws! authserver response if failure
;    (-> request
;      (assoc ::auth/auth-info
;             (and token auth))
;      (assoc ::auth/api-key token)
;      (assoc ::auth/authenticated-teams (and auth (teams/teams token))))))


(defn wrap-authenticated-teams
  "Wrap the response with a future authenticated-teams"
  [handler]
  (fn [request]
    (if (auth/authenticated? request)
      (-> request
        (assoc ::auth/authenticated-teams (teams/teams (auth/token request)))
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
