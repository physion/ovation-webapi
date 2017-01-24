(ns ovation.main
  (:gen-class)
  (:require [ring.adapter.jetty :as jetty]
            [ovation.handler]
            [ovation.config :as config]))

(defn -main []
  (ovation.logging/setup!)                                  ;; TODO Get a component system instead
  (jetty/run-jetty #'ovation.handler/app {:port config/PORT :join? false}))
