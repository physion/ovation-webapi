(ns ovation.middleware.token-auth
  "HTTP token authorization and authenticated user info middleware for ovation.io"
  (:require [ovation.auth :as auth]
            [clojure.string :refer [lower-case]]
            [ovation.teams :as teams]))

;; Original code from https://github.com/jstewart/ring-token-authentication/blob/master/src/ring/middleware/token_authentication.clj
;; Used under the EPL 1.0 license

(defn token-auth-request
  "Authenticates the given request against using a fn (f) that accepts a single
  string parameter that is the token parsed from the authentication header.
  The return value is added to the request with the keyword :token-authentication.
  true indicates successful auth, while false or nil indicates failure.
  failure."
  [request authserver]
  (let [auth ((:headers request) "authorization")
        token (and auth (last (re-find #"Bearer (.*)$" auth)))
        auth (auth/authenticate authserver token)]             ;; throws! authserver response if failure
    (-> request
      (assoc ::auth/auth-info
             (and token auth))
      (assoc ::auth/api-key token)
      (assoc ::auth/authenticated-teams (teams/teams token)))))

(defn token-authentication-failure
  "Returns a 401 unauthorized, along with body text that indicates the same.
   alternatively overridden by 'custom-response'
   (:status, :body, keys or :headers map must be supplied when overriding"
  [custom-response]
  (let [resp {:status 401 :body "unauthorized"}
        headers {"WWW-Authenticate" "Token realm=Application"}]
    (assoc (merge resp custom-response)
      :headers (merge (:headers resp) headers))))

(defn wrap-token-auth
  "Wrap the response with a REST token authentication. If the token is invalid, throws! auth response
  from authserver.
  Additionally, the WWW-Authenticate: header is set in accordance with token auth drafts."
  [handler & {:keys [custom-response authserver required-auth-url-prefix]}]
  (fn [request]
    (if (and required-auth-url-prefix
          (not (empty? (filter #(.startsWith (lower-case (:uri request)) (lower-case %)) required-auth-url-prefix))))
      (let [token-req (token-auth-request request authserver)]
        (if (::auth/auth-info token-req)
          (handler token-req)
          (token-authentication-failure custom-response)))
      (handler request))))
