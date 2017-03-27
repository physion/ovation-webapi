(ns ovation.audit
  (:require [ring.logger.messages :refer [request-details]]
            [ring.logger.protocols :refer [info]]
            [clojure.tools.logging]
            [ovation.auth :as auth]))


(defmethod request-details :identity-printer
  [{:keys [logger] :as options} req]
  (info logger (str "[AUDIT] " (merge {:email (get-in req [:identity :email])
                                       :identity (get-in req [:identity :sub])}
                                (select-keys req [:request-method
                                                  :uri])))))
