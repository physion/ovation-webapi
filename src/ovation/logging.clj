(ns ovation.logging
  (:require [clj-logging-config.log4j :as log-config]
            [ovation.config :refer [LOGGING_HOST LOGGING_NAME]]
            [slingshot.slingshot :refer [try+]])
  (:import (org.apache.log4j.net SyslogAppender)
           (org.apache.log4j PatternLayout SimpleLayout ConsoleAppender)
           (java.net UnknownHostException)))

(defn configure-console-logger!
  []
  (log-config/set-logger!
    :level :debug
    :out (ConsoleAppender. (SimpleLayout.))))

(defn setup! []
  (if-let [host LOGGING_HOST]
    (let [logging-name (or LOGGING_NAME "ovation")]
      (try+

        (let [papertrail (doto (SyslogAppender.)
                           (.setSyslogHost host)
                           (.setFacility "LOCAL7")
                           (.setFacilityPrinting false)
                           (.setName logging-name)
                           (.setLayout (PatternLayout. (str "%p: " logging-name " (%F:%L) %x %m %n"))))]
          (log-config/set-logger!
            :level :debug
            :out papertrail))

        (catch UnknownHostException _
          (configure-console-logger!))))

    (configure-console-logger!)))
