(ns ovation-api-webservice.handler
  (:use compojure.core
        ovation-api-webservice.entity-view
        ovation-api-webservice.user-view
        ring.middleware.params
  )
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.json :as middleware]
            [ovation-api-webservice.entity-view :as entity-view]
            [ovation-api-webservice.user-view :as user-view]
            [ring.middleware.cors :refer [wrap-cors]]
  )
)

(defroutes app-routes

  (context "/user" [] (defroutes index-routes
    (GET "/" {params :params} (index-user params))
;    (POST "/" {params :params} (create-user params))
    (context "/:id" [id] (defroutes index-routes
      (GET "/" {params :params} (get-user params))
;      (PUT "/" {params :params} (update-user params))
;      (DELETE "/" {params :params} (delete-user params))
    ))
  ))

  (context "/entity" [] (defroutes index-routes
    (POST "/" request (create-entity request))
    (context "/:id" [id] (defroutes index-routes
      (GET "/" request (get-entity id request))
      (PUT "/" request (update-entity id request))
      (DELETE "/" request (delete-entity id request))
      (context "/:rel" [rel] (defroutes index-routes
        (GET "/" request (get-entity-rel id rel request))
      ))
    ))
  ))

  (context "/:resource" [resource] (defroutes index-routes
    (GET "/" request (index-resource resource request))
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
                 :access-control-allow-methods [:get :put :post :delete :options]
                 :access-control-allow-headers ["Content-Type" "Accept"])))


;;(middleware/wrap-json-body)
;;
