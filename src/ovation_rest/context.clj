(ns ovation-rest.context
  (:import (java.net URI))
  (:require [clojure.core.memoize :as memo]))



(defn make-server
  "Make an us.physion.ovation.api.web.Server instance"
  [api-endpoint api-key]
  (us.physion.ovation.api.web.Server/make (URI. api-endpoint) api-key))

(defn make-context
  "Make an Ovation DataContext for the given API key"
  [api-key]

  (let [api-endpoint (if (System/getProperty "OVATION_IO_HOST_URI")
                       (System/getProperty "OVATION_IO_HOST_URI")
                       "https://dev.ovation.io")]

    (-> (make-server api-endpoint api-key)
        (.getContext))))


;;TODO lru/threshold should be parameterized
(def cached-context (memo/lru make-context {} :lru/threshold 3))
