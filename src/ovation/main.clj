(ns ovation.main
  (:require [ring.adapter.jetty :as jetty]
            [ovation.handler]
            [ovation.config :as config]))

(defn -main []
  (ovation.logging/setup!)
  (jetty/run-jetty #'ovation.handler/app {:port config/PORT :join? false}))
