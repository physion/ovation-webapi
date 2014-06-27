(ns ovation-api-webservice.handler
  (:use compojure.core
        ovation-api-webservice.project-view
        ovation-api-webservice.experiment-view
        ovation-api-webservice.epoch-view
        ovation-api-webservice.measurement-view
        ovation-api-webservice.protocol-view
        ovation-api-webservice.source-view
        ovation-api-webservice.user-view
        ring.middleware.params)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.json :as middleware]
            [ovation-api-webservice.project-view :as project-view]
            [ovation-api-webservice.experiment-view :as experiment-view]
            [ovation-api-webservice.epoch-view :as epoch-view]
            [ovation-api-webservice.measurement-view :as measurement-view]
            [ovation-api-webservice.protocol-view :as protocol-view]
            [ovation-api-webservice.source-view :as source-view]
            [ovation-api-webservice.user-view :as user-view]
            [ring.middleware.cors :refer [wrap-cors]]))

(defroutes app-routes
  (context "/project" [] (defroutes index-routes
    (GET "/" request (index-project request))
    (POST "/" request (create-project request))
    (context "/:id" [id] (defroutes index-routes
      (GET "/" request (get-project id request))
      (PUT "/" request (update-project id request))
      (DELETE "/" request (delete-project id request))
    ))
  ))

  (context "/experiment" [] (defroutes index-routes
    (GET "/" {params :params} (index-experiment params))
    (POST "/" {params :params} (create-experiment params))
    (context "/:id" [id] (defroutes index-routes
      (GET "/" {params :params} (get-experiment params))
      (PUT "/" {params :params} (update-experiment params))
      (DELETE "/" {params :params} (delete-experiment params))
    ))
  ))

  (context "/epoch" [] (defroutes index-routes
    (GET "/" {params :params} (index-epoch params))
    (POST "/" {params :params} (create-epoch params))
    (context "/:id" [id] (defroutes index-routes
      (GET "/" {params :params} (get-epoch params))
      (PUT "/" {params :params} (update-epoch params))
      (DELETE "/" {params :params} (delete-epoch params))
    ))
  ))

  (context "/measurement" [] (defroutes index-routes
    (GET "/" {params :params} (index-measurement params))
    (POST "/" {params :params} (create-measurement params))
    (context "/:id" [id] (defroutes index-routes
      (GET "/" {params :params} (get-measurement params))
      (PUT "/" {params :params} (update-measurement params))
      (DELETE "/" {params :params} (delete-measurement params))
    ))
  ))

  (context "/protocol" [] (defroutes index-routes
    (GET "/" {params :params} (index-protocol params))
    (POST "/" {params :params} (create-protocol params))
    (context "/:id" [id] (defroutes index-routes
      (GET "/" {params :params} (get-protocol params))
      (PUT "/" {params :params} (update-protocol params))
      (DELETE "/" {params :params} (delete-protocol params))
    ))
  ))

  (context "/source" [] (defroutes index-routes
    (GET "/" {params :params} (index-source params))
    (POST "/" {params :params} (create-source params))
    (context "/:id" [id] (defroutes index-routes
      (GET "/" {params :params} (get-source params))
      (PUT "/" {params :params} (update-source params))
      (DELETE "/" {params :params} (delete-source params))
    ))
  ))

  (context "/user" [] (defroutes index-routes
    (GET "/" {params :params} (index-user params))
;    (POST "/" {params :params} (create-user params))
    (context "/:id" [id] (defroutes index-routes
      (GET "/" {params :params} (get-user params))
;      (PUT "/" {params :params} (update-user params))
;      (DELETE "/" {params :params} (delete-user params))
    ))
  ))

  (route/resources "/")
  (route/not-found "Not Found")
)

;(def handler
;  (wrap-cors my-routes :access-control-allow-origin #"*"
;                       :access-control-allow-methods [:get :put :post :delete]))

(def app
  (-> (handler/site app-routes)
      (middleware/wrap-json-response)
      (wrap-cors :access-control-allow-origin #".+"; FIXME - accept only what we want here
                 :access-control-allow-methods [:get :put :post :delete]
                 :access-control-allow-headers ["Content-Type" "Accept"])))


;;(middleware/wrap-json-body)
;;
