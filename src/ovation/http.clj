(ns ovation.http
  (:require [ovation.util :as util]
            [clojure.tools.logging :as logging]
            [org.httpkit.client :as httpkit.client]
            [clojure.core.async :refer [chan go >! >!! pipeline]]
            [slingshot.slingshot :refer [try+]]
            [ring.util.http-response :refer [throw!]])
  (:import (java.io EOFException)))

(defn call-http
  "Makes http-client async request onto the provided channel.

  Conveys response body if success or a throw! in case of failure"
  [ch method url opts success-fn]
  (httpkit.client/request (merge {:method method :url url} opts)
    (fn [resp]
      (logging/info "Received HTTP response:" method url "-" (:status resp))
      (logging/debug "Raw:" (:body resp))
      (if (success-fn resp)
        (try+
          (let [body (util/from-json (:body resp))]
            (logging/debug "Response:" body)
            (>!! ch body))
          (catch EOFException _
            (logging/info "Response is empty")
            (>!! ch {})))
        (try+
          (logging/debug "Throwing HTTP response error for" resp)
          (throw! resp)
          (catch Object ex
            (logging/debug "Conveying HTTP response error" ex)
            (>!! ch ex)))))))
