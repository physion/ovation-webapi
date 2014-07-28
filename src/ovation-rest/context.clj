(ns ovation-rest.context
  (:require [clojure.core.memoize :as memo])
  (:import (java.net URI)))



(defn make-context [api_key]
  (-> (us.physion.ovation.api.web.Server/make (URI. "https://dev.ovation.io") api_key) (.getContext)))


(def cached-context (memo/lru make-context {} :lru/threshold 3))
