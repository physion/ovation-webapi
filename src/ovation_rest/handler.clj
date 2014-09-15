(ns ovation-rest.handler
  (:require [clojure.string :refer [join]]
            [ring.util.http-response :refer :all]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.swagger.schema :refer [field describe]]
            [ring.swagger.json-schema-dirty]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [pathetic.core :refer [url-normalize]]
            [ovation-rest.paths :as paths]
            [ovation-rest.entity :as entity]
            [ovation-rest.util :as util]
            ))

;;; --- Schema Definitions --- ;;;

(s/defschema Success {:success s/Bool})

(s/defschema Entity {:type                         s/Str    ;(s/enum :Project :Protocol :User :Source)
                     :_rev                         s/Str
                     :_id                          s/Str    ; could we use s/uuid here?

                     (s/optional-key :attributes)  {s/Keyword s/Str}

                     :links                        {s/Keyword #{s/Str}}
                     (s/optional-key :named_links) {s/Keyword {s/Keyword #{s/Str}}}

                     (s/optional-key :annotations) {s/Keyword {s/Keyword {s/Keyword #{{s/Keyword s/Str}}}}}
                     })

(s/defschema NewEntity (dissoc Entity :_id :_rev))


(s/defschema EntityList [Entity])


;;; --- Routes --- ;;;
(defapi app

        {:formats [:application/json]}

        (middlewares [(wrap-cors
                        :access-control-allow-origin #".+"  ; FIXME - accept only what we want here
                        :access-control-allow-methods [:get :put :post :delete :options]
                        :access-control-allow-headers ["Content-Type" "Accept"])]
                     (swagger-ui)
                     (swagger-docs
                       :apiVersion "1.0.0"
                       :title "Ovation"
                       :description "Ovation Web API"
                       :contact "support@ovation.io"
                       :termsOfServicdUrl "https://ovation.io/terms_of_service")

                     (swaggered "top-level"
                                (context "/api" []
                                         (context "/v1" []
                                                  (context "/:resource" [resource]
                                                           (GET* "/" request
                                                                 :return [Entity]
                                                                 :query-params [api-key :- String]
                                                                 :path-params [resource :- (s/enum "projects" "sources" "protocols")]
                                                                 :summary "Get Projects, Protocols and Top-level Sources"

                                                                 (ok (entity/index-resource api-key resource (util/host-context request))))))))

                     (swaggered "entities"
                                (context "/api" []
                                         (context "/v1" []
                                                  (context "/entities" []
                                                           (POST* "/" request
                                                                  :return [Entity]
                                                                  :query-params [api-key :- String]
                                                                  :body [new-dto NewEntity]
                                                                  :summary "Creates and returns an entity"
                                                                  (ok (entity/create-entity api-key new-dto (util/host-context request))))
                                                           (context "/:id" [id]
                                                                    (GET* "/" request
                                                                          :return [Entity]
                                                                          :query-params [api-key :- String]
                                                                          :summary "Returns entity with :id"
                                                                          (ok (entity/get-entity api-key id (util/host-context request :remove-levels 1))))
                                                                    (PUT* "/" request
                                                                          :return [Entity]
                                                                          :query-params [api-key :- String]
                                                                          :body [dto Entity]
                                                                          :summary "Updates and returns updated entity with :id"
                                                                          (ok (entity/update-entity api-key id dto (util/host-context request :remove-levels 1))))
                                                                    (DELETE* "/" request
                                                                             :return Success
                                                                             :query-params [api-key :- String]
                                                                             :summary "Deletes entity with :id"
                                                                             (ok (entity/delete-entity api-key id))))))))

                     (swaggered "views"
                                (context "/api" []
                                         (context "/v1" []
                                                  (context "/views" []
                                                           (GET* "/*" request
                                                                 :return [Entity]
                                                                 :query-params [api-key :- String]
                                                                 :summary "Returns entities in view. Views follow CouchDB calling conventions (http://wiki.apache.org/couchdb/HTTP_view_API)"
                                                                 (let [host (util/host-from-request request)]
                                                                   (ok (entity/get-view api-key
                                                                                        (url-normalize (format "%s/%s?%s" host (:uri request) (util/ovation-query request)))
                                                                                        (util/host-context request :remove-levels 1)))))))))))

