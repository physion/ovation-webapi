(ns ovation.logging
  (:require [taoensso.timbre :as timbre]
            ;[taoensso.timbre.appenders.3rd-party.logstash :as logstash-appender]
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


;; copied from taoensso.timbre.appenders.3rd-party.logstash until it's in the JAR

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

(def iso-format "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

(defn data->json-stream
  [data writer opts]
  ;; Note: this it meant to target the logstash-filter-json; especially "message" and "@timestamp" get a special meaning there.
  (let [stacktrace-str (if-let [pr (:pr-stacktrace opts)]
                         #(with-out-str (pr %))
                         timbre/stacktrace)]
    (cheshire/generate-stream
      (merge (:context data)
        {:level (:level data)
         :namespace (:?ns-str data)
         :file (:?file data)
         :line (:?line data)
         :stacktrace (some-> (force (:?err_ data)) (stacktrace-str))
         :hostname (force (:hostname_ data))
         :message (force (:msg_ data))
         "@timestamp" (:instant data)})
      writer
      (merge {:date-format iso-format
              :pretty false}
        opts))))

(defn logstash-appender
  "Returns a Logstash appender, which will send each event in JSON
  format to the logstash server at `host:port`. Additionally `opts`
  may be a map with `:pr-stracktrace` mapped to a function taking an
  exception, which should write the stacktrace of that exception to
  `*out`."
  [host port & [opts]]
  (let [conn (atom nil)
        nl "\n"]
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
                                            (connect host port))))]
                       (locking sock
                         (data->json-stream data out (select-keys opts [:pr-stacktrace]))
                         ;; logstash tcp input plugin: "each event is assumed to be one line of text".
                         (.write ^java.io.Writer out nl)))
                     (catch java.io.IOException _
                       nil)))}))


(defn logging-config
  []
  (if-let [host config/LOGGING_HOST]
    (when-let [port config/LOGGING_PORT]
      {:level     :info
       :appenders {:timbre (timbre-json-appender host port)
                   :println (timbre/println-appender {:stream :auto})}})
    {:level     :info}))
     ;:appenders {:println (timbre/println-appender {:stream :auto})}


(defn setup! []
  (timbre/set-config! (logging-config)))
