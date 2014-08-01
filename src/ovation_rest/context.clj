(ns ovation-rest.context
  (:import (java.net URI))
  (:require [clojure.core.memoize :as memo]))



(defn make-context
  "Make an Ovation DataContext for the given API key"
  [api_key]

  (let [api_endpoint (if (System/getenv "OVATION_IO_HOST_URI")
                       (System/getenv "OVATION_IO_HOST_URI")
                       "https://dev.ovation.io")]

    (-> (us.physion.ovation.api.web.Server/make (URI. api_endpoint) api_key)
        (.getContext))))


;;TODO lru/threshold should be parameterized
(def cached-context (memo/lru make-context {} :lru/threshold 3))
