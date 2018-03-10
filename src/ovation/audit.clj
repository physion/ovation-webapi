(ns ovation.audit
  (:require [ring.logger.messages :refer [request-details]]
            [ring.logger.protocols :refer [info Logger]]
            [clojure.tools.logging]
            [clojure.tools.logging :as logging]))


(def ^:dynamic *request-id* nil)

(defn add-request-id [handler]
  (fn [request]
    (binding [*request-id* (rand-int 0xffff)]
      (handler request))))

(defn make-logger []
  (reify Logger
    (add-extra-middleware [_ handler] handler)
    (log [_ level throwable message]
      (logging/log level throwable (format "(%04x) %s" *request-id* message)))))

(defmethod request-details :audit-printer
  [{:keys [logger] :as options} req]
  (let [audit-msg (str "[AUDIT] " (merge {:sub (get-in req [:identity :sub])}
                                    (select-keys req [:request-method
                                                      :remote-addr
                                                      :uri])))]
    (info logger audit-msg)))
