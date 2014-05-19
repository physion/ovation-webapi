(ns ovation-api-webservice.handler
  (:use compojure.core
        ovation-api-webservice.view)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ovation-api-webservice.view :as view]))

(defroutes app-routes
  (GET "/" [] "Hello World")
  (GET "/project" [] (index-project))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
