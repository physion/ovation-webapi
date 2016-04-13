(ns ovation.logging
  (:require [taoensso.timbre :as timbre]
            [potemkin :refer [import-vars]]
            [ovation.config :as config]
            [cheshire.core :as cheshire]
            [clj-time.coerce :as time-coerce]
            [clj-time.format :as time-format])
  (:import [java.net Socket InetAddress]
           [java.io PrintWriter]))

(import-vars
  [taoensso.timbre
   log debug info warn error fatal])





;; Adapted from taoensso.timbre.appenders.3rd-party.server-socket
;; Adapted from active-group/timbre-logstash
(defn connect
  [host port]
  (let [addr (InetAddress/getByName host)
        sock (Socket. addr (int port))]
    [sock
     (PrintWriter. (.getOutputStream sock))]))

(defn connection-ok?
  [[^Socket sock ^PrintWriter out]]
  (and (not (.isClosed sock))
    (.isConnected sock)
    (not (.checkError out))))

(def iso-formatter (time-format/formatters :basic-date-time))

(defn data->json-string
  [data]
  (cheshire/generate-string
    (merge (:context data)
      {:throwable    (some-> (:throwable data) timbre/stacktrace)
       :level        (:level data)
       :error-level? (:error-level? data)
       :?ns-str      (:?ns-str data)
       :?file        (:?file data)
       :?line        (:?line data)
       :err          (str (force (:?err_ data)))
       :vargs        (force (:vargs_ data))
       :hostname     (force (:hostname_ data))
       :msg          (force (:msg_ data))
       :timestamp    (time-format/unparse iso-formatter (time-coerce/from-date (:instant data)))})))

(defn timbre-json-appender
  "Returns a Logstash appender."
  [host port]
  (let [conn (atom nil)]
    {:enabled?   true
     :async?     false
     :min-level  nil
     :rate-limit nil
     :output-fn  :inherit
     :fn
                 (fn [data]
                   (try
                     (let [[sock out] (swap! conn
                                        (fn [conn]
                                          (or (and conn (connection-ok? conn) conn)
                                            (connect host port))))
                           json (data->json-string data)]
                       (binding [*out* out]
                         (println json)))
                     (catch java.io.IOException _
                       nil)))}))


(defn logging-config
  []
  (if-let [host config/LOGGING_HOST]
    (let [port config/LOGGING_PORT]
      {:level     :info
       :appenders {:timbre (timbre-json-appender host port)}})
    {:level     :info
     :appenders {:println (timbre/println-appender {:stream :auto})}
     }))

(defn setup! []
  (timbre/set-config! (logging-config)))
