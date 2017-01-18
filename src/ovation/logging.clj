(ns ovation.logging
  (:require [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders]
            [taoensso.timbre.appenders.3rd-party.logstash :refer [logstash-appender]]
            [taoensso.timbre.appenders.3rd-party.logstash :refer [logstash-appender]]
            [potemkin :refer [import-vars]]
            [ovation.config :as config]))

(import-vars
  [taoensso.timbre
   log debug info warn error fatal])


(defn logging-config
  []
  (if-let [host config/LOGGING_HOST]
    (let [port config/LOGGING_PORT]
      {:level     :info
       :output-fn (partial timbre/default-output-fn {:stacktrace-fonts {}})
       :appenders {:timbre (logstash-appender host port)
                   :println (appenders/println-appender {:stream :auto})}})

    {:level     :info
     :appenders {:println (appenders/println-appender {:stream :auto})
                 :file    (appenders/spit-appender {:fname "/var/app/current/ovation.log"})}}))


(defn setup! []
  (timbre/set-config! (logging-config)))
