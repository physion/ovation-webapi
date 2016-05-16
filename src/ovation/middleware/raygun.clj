(ns ovation.middleware.raygun
  (:require [slingshot.slingshot :refer [try+ throw+]])
  (:import [com.mindscapehq.raygun4java.core RaygunClient]
           (clojure.lang ExceptionInfo)))

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
        (catch [:type :ring.util.http-response/response] e
          (let [ex (-> e
                     (assoc-in [:response :opts :body] "REDACTED")
                     (assoc-in [:response :body] "REDACTED"))
                (.Send (RaygunClient. api-key) ex [] (raygun-params request))
                (throw+ e)])))
      (handler request))))
