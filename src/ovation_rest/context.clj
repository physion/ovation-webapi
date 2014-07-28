(ns ovation-rest.context
  (:import (java.net URI))
  (:require [clojure.core.memoize :as memo]))



(defn make-context [api_key]
  (-> (us.physion.ovation.api.web.Server/make (URI. "https://dev.ovation.io") api_key) (.getContext))) ;;TODO endpoint needs to be parametrized


(def cached-context (memo/lru make-context {} :lru/threshold 3))
