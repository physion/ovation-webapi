(ns ovation.main
  (:require [ring.adapter.jetty :as jetty]
            [ovation.handler]
            [ovation.config :as config]))

(defn -main []
  (jetty/run-jetty #'ovation.handler/app {:port (config/PORT) :join? false}))
