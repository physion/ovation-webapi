(ns ovation.http
  (:require [ovation.util :as util]
            [clojure.tools.logging :as logging]
            [org.httpkit.client :as httpkit.client]
            [clojure.core.async :refer [chan go >! >!! pipeline]]
            [slingshot.slingshot :refer [try+]]
            [ring.util.http-response :refer [throw!]]))

(defn call-http
  "Makes http-client async request onto the provided channel.

  Conveys response body if success or a throw! in case of failure"
  [ch method url opts success-fn]
  (httpkit.client/request (merge {:method method :url url} opts)
    (fn [resp]
      (logging/info "Received HTTP response:" url "-" (:status resp))
      (if success-fn
        (>!! ch (util/from-json (:body resp)))
        (try+
          (throw! resp)
          (catch Object ex
            (>!! ch ex)))))))
