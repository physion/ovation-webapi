(ns ovation.logging
  (:require [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders]
            [taoensso.timbre.appenders.3rd-party.logstash :refer [logstash-appender]]
            [taoensso.timbre.appenders.3rd-party.logstash :refer [logstash-appender]]
            [potemkin :refer [import-vars]]
            [ovation.config :as config]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]))

(import-vars
  [taoensso.timbre
   log debug info warn error fatal])

(def log4j-appender-fn "Timbre -> Log4j appender :fn"
  (let [log4j-factory (clojure.tools.logging.impl/log4j-factory)
        levels        #{:trace, :debug, :info, :warn, :error, :fatal}]
    (fn [{:keys [hostname_ timestamp_ ?err_ ?ns-str level output-fn] :as data}]
      (log/log log4j-factory
        (or ?ns-str "?ns")
        (get levels level :info)
        (or (force ?err_) nil)
        (output-fn data)))))

(def log4j-appender "Timber appender which outputs to log4j."
  {:enabled?  true
   :async?    false
   :min-level :info
   :output-fn (fn [{:keys [msg_]}] (str (force msg_)))
   :fn        log4j-appender-fn})


(defn logging-config
  []
  (let [host config/LOGGING_HOST
        port config/LOGGING_PORT]
    (if (and host port)
      (timbre/merge-config! {:level     :info
                             :output-fn (partial timbre/default-output-fn {:stacktrace-fonts {}})
                             :appenders {:logstash   (logstash-appender host port)
                                         :println    (appenders/println-appender {:stream :auto})
                                         :papertrail (assoc log4j-appender
                                                       :enabled? (config/config :log-to-papertrail))}})

      {:level     :info
       :appenders {:println (appenders/println-appender {:stream :auto})}})))


(defrecord Logging []
  component/Lifecycle

  (start [this]
    (timbre/set-config! (logging-config)))
  (stop [this]))
