(ns ovation.middleware.raygun
  (:require [slingshot.slingshot :refer [try+ throw+]]
            [ovation.auth :as auth])
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
        (catch Object e
          (doto (RaygunClient. api-key)
            (.SetUser (auth/authenticated-user-id (auth/identity request)))
            (.Send nil [] (raygun-params request)))
          (throw+ e)))

      (handler request))))
