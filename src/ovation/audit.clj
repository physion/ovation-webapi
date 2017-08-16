(ns ovation.audit
  (:require [ring.logger.messages :refer [request-details]]
            [ring.logger.protocols :refer [info]]
            [clojure.tools.logging]
            [clojure.tools.logging :as logging]))


(defmethod request-details :audit-printer
  [{:keys [logger] :as options} req]
  (let [audit-msg (str "[AUDIT] " (merge {:sub (get-in req [:identity :sub])}
                                    (select-keys req [:request-method
                                                      :remote-addr
                                                      :uri
                                                      :body])))]
    (logging/info audit-msg)
    (info logger audit-msg)))
