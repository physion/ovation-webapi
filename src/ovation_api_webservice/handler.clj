(ns ovation-api-webservice.handler
  (:use compojure.core
        ovation-api-webservice.view
        ring.middleware.params)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ovation-api-webservice.view :as view]
            [ring.middleware.cors :refer [wrap-cors]]))

(defroutes app-routes
  (GET "/" [] "nothing to see here, move along")
  (GET "/project" {params :params} (index-project params))
  (route/resources "/")
  (route/not-found "Not Found"))

;(def handler
;  (wrap-cors my-routes :access-control-allow-origin #"*"
;                       :access-control-allow-methods [:get :put :post :delete]))

(def app
  (-> (handler/site app-routes)
      (wrap-cors :access-control-allow-origin #"http://localhost:9000")))
