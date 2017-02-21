(ns ovation.audit
  (:require [ring.logger.messages :refer [request-details]]
            [ring.logger.protocols :refer [info]]
            [clojure.tools.logging]
            [ovation.auth :as auth]))


(defmethod request-details :identity-printer
  [{:keys [logger] :as options} req]
  (info logger (str "[AUDIT] " (merge {:identity (get-in req [:identity :uuid])}
                                (select-keys req [:request-method
                                                  :uri])))))

(defn audit
  "Logs audit for request"
  [request body]
  (clojure.tools.logging/info "[AUDIT] " {:identity (auth/authenticated-user-id (auth/identity request))} body))
