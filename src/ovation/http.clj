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
      (logging/info "Received HTTP response:" method url (:query-params opts) "-" (:status resp))
      (logging/debug "Raw:" (:body resp))
      (if (success-fn resp)
        (try+
          (let [body (util/from-json (:body resp))]
            (logging/debug "Response:" body)
            (>!! ch body))
          (catch EOFException _
            (logging/info "Response is empty")
            (let [err {:type :ring.util.http-response/response :response resp}]
              (logging/debug "Conveying HTTP response error " err)
              (>!! ch err))))

        (let [err {:type :ring.util.http-response/response :response resp}]
          (logging/debug "Conveying HTTP response error " err)
          (>!! ch err))))))
