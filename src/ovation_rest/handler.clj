(ns ovation-rest.handler
  (:use compojure.core
        ring.middleware.params
        )
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.json :as middleware]
            [ovation-rest.entity :as entity]
            [ovation-rest.user :as user]
            [ring.middleware.cors :refer [wrap-cors]]
            )
  )

(defroutes app-routes

           (context "/user" [] (defroutes index-routes
                                          ; POST not allowed
                                          (GET "/" {params :params} (user/index-user params))
                                          (context "/:id" [id] (defroutes index-routes
                                                                          ; PUT, DELETE not allowed
                                                                          (GET "/" {params :params} (user/get-user params))))))

           (context "/entity" [] (defroutes index-routes
                                            (POST "/" request (entity/create-entity request))
                                            (context "/:id" [id] (defroutes index-routes
                                                                            (GET "/" request (entity/get-entity id request))
                                                                            (PUT "/" request (entity/update-entity id request))
                                                                            (DELETE "/" request (entity/delete-entity id request))
                                                                            (context "/:rel" [rel] (defroutes index-routes
                                                                                                              (GET "/" request (entity/get-entity-rel id rel request))))))))

           (context "/:resource" [resource] (defroutes index-routes
                                                       (GET "/" request (entity/index-resource resource request))))

           (GET "/" [] "Ovation REST API")
           (route/not-found "<h1>Not Found</h1>"))



(def app
  (-> (handler/site app-routes)
      (middleware/wrap-json-response)
      (wrap-cors :access-control-allow-origin #".+"         ; FIXME - accept only what we want here
                 :access-control-allow-methods [:get :put :post :delete :options]
                 :access-control-allow-headers ["Content-Type" "Accept"])))
