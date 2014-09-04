(ns ovation-rest.handler
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [ring.swagger.schema :refer [field describe]]
            [ovation-rest.entity :as entity]
            [ring.swagger.json-schema-dirty]))


;{"_rev":"2-d330d60c5d22eeeb85dfb8ef4208055d",
;"_id":"3dc9e647-9ecc-4fac-9f21-3d5cd08621e4",
;"links":{"empty":false},
;"named_links":{},
;"annotations":{"properties":{"ovation://entities/1bb679e6-c853-4d73-8448-791e839f0077":[{"key":"key","value":"value"}]}},
;"attributes":{"start":"2014-07-23T01:30:06.479Z",
;              "name":"name","purpose":"purpose"},
;"type":"Project"}

;;; --- Schema Definitions --- ;;;

(s/defschema Success {:success 1})

(s/defschema Entity {
                     :type (s/enum :Project :Protocol :User :Source)
                     :_rev s/Str
                     :_id s/Str
                     :links {}
                     :attributes {(s/optional-key String) String}
                     :named_links {(s/optional-key String) String}
                     :test {(s/optional-key String) (s/maybe {})}
; WHY DOESNT THIS WORK :(                     :annotations {(s/optional-key String) {(s/optional-key String) {(s/optional-key String) [{(s/optional-key String) String}]}}}
                    }
)


;;; --- Routes --- ;;;

(defroutes* head-ping
  (HEAD* "/" [] ""))

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
    head-ping

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

  )
)

