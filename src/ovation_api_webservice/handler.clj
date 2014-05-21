(ns ovation-api-webservice.handler
  (:use compojure.core
        ovation-api-webservice.view
        ring.middleware.params)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ovation-api-webservice.view :as view]))

(defroutes app-routes
  (GET "/" [] "nothing to see here, move along")
  (GET "/project" {params :params} (index-project params))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
