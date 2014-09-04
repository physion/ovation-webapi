(ns ovation-rest.handler
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [ring.swagger.schema :refer [field describe]]
            [ovation-rest.entity :as entity]
            [ovation-rest.user :as user]
            [schema.core :as s]))

;(ns ovation-rest.handler
;  (:use compojure.core
;        ring.middleware.params)
;  (:require [compojure.handler :as handler]
;            [compojure.route :as route]
;            [ring.middleware.json :as middleware]
;            [ovation-rest.entity :as entity]
;            [ovation-rest.user :as user]
;            [ring.middleware.cors :refer [wrap-cors]]
;            [ring.util.http-response :refer :all]
;            [compojure.api.sweet :refer :all]
;            [schema.core :as s]))

;{"_rev":"2-d330d60c5d22eeeb85dfb8ef4208055d",
;"_id":"3dc9e647-9ecc-4fac-9f21-3d5cd08621e4",
;"links":{"empty":false},
;"named_links":{},
;"annotations":{"properties":{"ovation://entities/1bb679e6-c853-4d73-8448-791e839f0077":[{"key":"key","value":"value"}]}},
;"attributes":{"start":"2014-07-23T01:30:06.479Z",
;              "name":"name","purpose":"purpose"},
;"type":"Project"}

(s/defschema Success {:success 1})

(s/defschema Entity {
;                     :type (s/enum :Project :Protocol :User :Source) 
                     :_rev String
                     :_id String
                     :links {}
                     :named_links {}
                     :annotations {}
                    }
)


(defapi app
;  (middleware/wrap-json-response)
;  (wrap-cors :access-control-allow-origin #".+"         ; FIXME - accept only what we want here
;             :access-control-allow-methods [:get :put :post :delete :options]
;             :access-control-allow-headers ["Content-Type" "Accept"])
  (swagger-ui)
  (swagger-docs
    :title "ovation-api-webservice"
    :description "Ovation API Webservice")

  (swaggered "ovation-api-webservice"
    :description "Ovation API Webservice"

    (context "/api" []
      (context "/entity" []
        (POST* "/" request
          :return        [Entity]
          :query-params  [api-key :- String]
          :summary       "Creates and returns an entity"
          (ok (entity/create-entity request)))
        (context "/:id" [id]
          (GET* "/" request
            :return        [Entity]
            :query-params  [api-key :- String]
            :summary       "Returns entity with :id"
            (ok (entity/get-entity id request)))
          (PUT* "/" request
            :return        [Entity]
            :query-params  [api-key :- String]
            :summary       "Updates and returns updated entity with :id"
            (ok (entity/update-entity id request)))
          (DELETE* "/" request
            :return        Success
            :query-params  [api-key :- String]
            :summary       "Deletes entity with :id"
            (ok (entity/update-entity id request)))
        )
      )
    )

    (context "/:resource" [resource]
      (GET* "/" request 
        :return        [Entity]
        :query-params  [api-key :- String]
        :summary       "Special endpoint for /project /protocol /source"
        (ok (entity/index-resource resource request)))
    )

;    (HEAD "/" [] "")

;    (GET* "/" []
;      :return         s/Str
;      :query-params   []
;      :summary        "Default return"
;      (ok "")
;    )
  )
)

;      (context "/user" [] 
;        (GET* "/" [] ;{params :params}
;          :return       [{:user User}]
;          :query-params [api-key :- String]
;          :summary      "returns all Users"
;          (ok {:user "all users"}))
;;          (ok {:user (user/index-user params})))
;        (GET* "/:id" [id] ;{params :params}
;          :return User
;          :query-params [api-key :- String]
;          :summary      "returns a User"
;          (ok {:user "one user"}))
;;          (ok {:user (user/get-user params}))))

;(defroutes app-routes
;
;           (context "/user" [] (defroutes index-routes
;                                          ; POST not allowed
;                                          (GET "/" {params :params} (user/index-user params))
;                                          (context "/:id" [id] (defroutes index-routes
;                                                                          ; PUT, DELETE not allowed
;                                                                          (GET "/" {params :params} (user/get-user params))))))
;
;           (context "/entity" [] (defroutes index-routes
;                                   (POST "/" request (entity/create-entity request))
;                                   (context "/:id" [id] (defroutes index-routes
;                                    (GET "/" request (entity/get-entity id request))
;                                    (PUT "/" request (entity/update-entity id request))
;                                    (DELETE "/" request (entity/delete-entity id request))
;                                    (context "/:rel" [rel] (defroutes index-routes
;                                      (GET "/" request (entity/get-entity-rel id rel request))))))))
;
;           (context "/:resource" [resource] (defroutes index-routes
;                                                       (GET "/" request (entity/index-resource resource request))))
;
;           (GET "/" [] "Ovation REST API")
;           (route/not-found "<h1>Not Found</h1>"))
;
;
;(def app
;  (-> (handler/site app-routes)
;      (middleware/wrap-json-response)
;      (wrap-cors :access-control-allow-origin #".+"         ; FIXME - accept only what we want here
;                 :access-control-allow-methods [:get :put :post :delete :options]
;                 :access-contol-allow-headers ["Content-Type" "Accept"])))
