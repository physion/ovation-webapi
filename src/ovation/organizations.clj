(ns ovation.organizations
  (:require [ovation.routes :as routes]
            [ovation.util :as util]
            [ovation.config :as config]
            [org.httpkit.client :as httpkit.client]
            [clojure.tools.logging :as logging]
            [ring.util.http-predicates :as http-predicates]
            [ring.util.http-response :refer [throw! bad-request! not-found! unprocessable-entity!]]
            [ovation.auth :as auth]
            [ovation.constants :as k]
            [ovation.request-context :as rc]
            [clojure.core.async :refer [chan >!!]]
            [slingshot.support :refer [get-throwable]]
            [ring.util.http-predicates :as hp]))

(defn request-opts
  [token]
  {:timeout     10000                                          ; ms
   :oauth-token token
   :headers     {"Content-Type" "application/json; charset=utf-8"}})

(defn read-tf
  [org]
  org)

(defn get-organizations
  "Gets all organizations for authenticated user as a future list: @[..org1.., ..org2..]"
  [ctx])
