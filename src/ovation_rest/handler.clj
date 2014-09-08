(ns ovation-rest.handler
  (:require [ring.util.http-response :refer :all]
            [ring.middleware.cors :refer [wrap-cors]]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [ring.swagger.schema :refer [field describe]]
            [ovation-rest.entity :as entity]
            [ring.swagger.json-schema-dirty]))

;;; --- Schema Definitions --- ;;;

(s/defschema Success {:success 1})

(s/defschema Entity {
                     :type (s/enum :Project :Protocol :User :Source)
                     :_rev s/Str
                     :_id s/Str
                     :links {s/Any s/Any}
                     :attributes {(s/optional-key String) String}
                     :named_links {(s/optional-key String) String}
                     :annotations {}
                     ;:annotations {(s/optional-key String) {(s/optional-key String) {(s/optional-key String) [{(s/optional-key String) String}]}}}
                    }
)


;;; --- Routes --- ;;;

(defroutes* head-ping
  (HEAD* "/" [] ""))

(defapi app

  ;(wrap-cors :access-control-allow-origin #".+"         ; FIXME - accept only what we want here
  ;           :access-control-allow-methods [:get :put :post :delete :options]
  ;           :access-control-allow-headers ["Content-Type" "Accept"])
;  (swagger-ui)
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
;        (ok [{:type :Project :_rev "123" :_id "123" :links {} :attributes {} :named_links {} :annotations {}}])))
;        (ok (entity/index-resource resource request))))
         (ok (entity/index-resource-helper "project" api-key))))

    (ANY* "*" [] (not-found "Illegal path"))
  )
)

