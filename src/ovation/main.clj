(ns ovation.main
  (:require [ring.adapter.jetty :as jetty]
            [ovation.handler]))

(defn -main []
  (jetty/run-jetty #'ovation.handler/app {:port 8080 :join? false}))
