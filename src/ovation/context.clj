(ns ovation.context
  (:import (java.net URI)
           (us.physion.ovation.exceptions OvationException AuthenticationException))
  (:require [clojure.core.memoize :as memo]
            [slingshot.slingshot :refer [try+ throw+]]
            [ring.util.http-response :refer [unauthorized! forbidden!]]
            [clojure.tools.logging :as logging]
            ))



(defn- make-server-helper
  [api-endpoint api-key]
  (us.physion.ovation.api.server.Server/make (URI. api-endpoint) api-key))

(defn make-server
  "Make an us.physion.ovation.api.web.Server instance"
  [api-endpoint api-key]
  (try+
    (make-server-helper api-endpoint api-key)
    (catch AuthenticationException _
      (unauthorized! "Authentication failed. Check your API key."))))

(defn- get-context-from-dsc
  "Gets a new DataContext from a DataStoreCoordinator"
  [dsc]
  (.getContext dsc))

(defn make-context
  "Make an Ovation DataContext for the given API key"
  [api-key]

  (let [api-endpoint (if-let [host (or (System/getProperty "OVATION_IO_HOST_URI") (System/getenv "OVATION_IO_HOST_URI"))]
                       host
                       "https://dev.ovation.io")]

    (get-context-from-dsc (make-server api-endpoint api-key))))

(def DEFAULT_LRU_THRESHOLD 5)
(def LRU_THRESHOLD "LRU_THRESHOLD_PROPERTY")

(def cached-context (memo/lru make-context {} :lru/threshold (if-let [threshold (System/getProperty LRU_THRESHOLD)]
                                                               (Integer/parseInt threshold)
                                                               DEFAULT_LRU_THRESHOLD)))
(defn clear-context-cache!
  "Clears the context cahce"
  []
  (memo/memo-clear! cached-context))

(defn begin-transaction
  [ctx]
  (.beginTransaction ctx))

(defn commit-transaction
  [ctx]
  (.commitTransaction ctx))

(defn abort-transaction
  [ctx]
  (.abortTransaction ctx))

(defmacro transaction
  "Wraps body in a DataContext transaction. Exceptions are rethrown."
  [context & body]
  `(let [context# ~context]
     (try+
       (begin-transaction context#)
       (let [result# (do ~@body)]
         (commit-transaction context#)
         result#)
       (catch OvationException _#
         (abort-transaction context#)
         (throw+)))))
