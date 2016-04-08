(ns ovation.logging
  (:require [taoensso.timbre :as timbre]
            [potemkin :refer [import-vars]]))

(import-vars
  [taoensso.timbre
   log debug info warn error fatal])


(def logging-config
  {:level     :info                                         ; e/o #{:trace :debug :info :warn :error :fatal :report}
   :appenders {:println (timbre/println-appender {:stream :auto})}})

(defn setup! []
  (timbre/set-config! logging-config))

;(if-let [host LOGGING_HOST]
;  (let [logging-name (or LOGGING_NAME "ovation")]
;    (try+
;
;      (let [papertrail (doto (SyslogAppender.)
;                         (.setSyslogHost host)
;                         (.setFacility "LOCAL7")
;                         (.setFacilityPrinting false)
;                         (.setName logging-name)
;                         (.setLayout (PatternLayout. (str "%p: " logging-name " (%F:%L) %x %m %n"))))]
;        (log-config/set-logger!
;          :level :debug
;          :out papertrail))
;
;      (catch UnknownHostException _
;        (configure-console-logger!))))
;
;  (configure-console-logger!)))
