(ns ovation-rest.context
    (:import (java.net URI))
    (:require [clojure.core.memoize :as memo]))



(defn make-context
      "Make an Ovation DataContext for the given API key"
      [api_key]
      ;;TODO let-if OVATION_IO_HOST_URI => default https://dev.ovation.io
      (-> (us.physion.ovation.api.web.Server/make (URI. (System/getenv "OVATION_IO_HOST_URI")) api_key)
          (.getContext)))


;;TODO lru/threshold should be parameterized
(def cached-context (memo/lru make-context {} :lru/threshold 3))
