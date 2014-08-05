(ns ovation-rest.context
  (:import (java.net URI))
  (:require [clojure.core.memoize :as memo]))



(defn make-server
  "Make an us.physion.ovation.api.web.Server instance"
  [api-endpoint api-key]
  (us.physion.ovation.api.web.Server/make (URI. api-endpoint) api-key))

(defn get-context-from-dsc
  "Gets a new DataContext from a DataStoreCoordinator"
  [dsc]
  (.getContext dsc))

(defn make-context
  "Make an Ovation DataContext for the given API key"
  [api-key]

  (let [api-endpoint (if (System/getProperty "OVATION_IO_HOST_URI")
                       (System/getProperty "OVATION_IO_HOST_URI")
                       "https://dev.ovation.io")]

    (get-context-from-dsc (make-server api-endpoint api-key))))


(def DEFAULT_LRU_THRESHOLD 5)
(def LRU_THRESHOLD "LRU_THRESHOLD_PROPERTY")
(def cached-context (memo/lru make-context {} :lru/threshold (if-let [threshold (System/getProperty LRU_THRESHOLD)]
                                                               (Integer/parseInt threshold)
                                                               DEFAULT_LRU_THRESHOLD)))
