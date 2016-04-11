(ns ovation.audit
  (:require [ring.logger.messages :refer [request-details]]
            [ovation.logging :refer [info]]
            [ovation.auth :as auth]))


(defmethod request-details :identity-printer
  [{:keys [logger] :as options} req]
  (ovation.logging/info "[AUDIT]" (merge {:identity (get-in req [:identity :uuid])}
                                    (select-keys req [:request-method
                                                      :uri]))))

(defn audit
  "Logs audit for request"
  [request body]
  (info "[AUDIT]" {:identity (auth/authenticated-user-id (auth/identity request))} body))
