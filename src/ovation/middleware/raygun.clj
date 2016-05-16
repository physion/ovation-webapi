(ns ovation.middleware.raygun
  (:require [slingshot.slingshot :refer [try+ throw+]]))

;; Forked from the https://github.com/thegreatape/ring-raygun project under the EPL

(defn raygun-params
  [request]
  (select-keys
    request
    [:server-port
     :server-name
     :remote-addr
     :uri
     :query-string
     :scheme
     :request-method
     :headers]))

(defn wrap-raygun-handler
  [handler api-key]
  (fn [request]
    (if api-key
      (try+
        (handler request)
        (catch Exception e
          (.Send (com.mindscapehq.raygun4java.core.RaygunClient. api-key) (ex-info "[REDACTED]" {}) [] (raygun-params request))

          (throw+ e)))

      (handler request))))
