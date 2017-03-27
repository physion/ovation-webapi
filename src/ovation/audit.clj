(ns ovation.audit
  (:require [ring.logger.messages :refer [request-details]]
            [ring.logger.protocols :refer [info]]
            [clojure.tools.logging]))


(defmethod request-details :identity-printer
  [{:keys [logger] :as options} req]
  (info logger (str "[AUDIT] " (merge {:email (get-in req [:identity :email])}
                                (select-keys req [:request-method
                                                  :uri])))))
