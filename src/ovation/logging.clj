(ns ovation.logging
  (:require [clj-logging-config.log4j :as log-config]
            [ovation.config :refer [LOGGING_HOST]]))

(defn setup! []
  (if-let [log-host LOGGING_HOST]
    (log-config/set-logger!
      :level :debug
      :out (org.apache.log4j.net.SyslogAppender.
             (org.apache.log4j.PatternLayout. "%p: (%F:%L) %x %m %n")
             log-host
             org.apache.log4j.net.SyslogAppender/LOG_LOCAL7))))
