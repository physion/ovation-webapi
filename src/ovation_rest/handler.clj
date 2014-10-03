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
                     :links                        {s/Keyword s/Str}
                     :attributes                   {s/Keyword s/Str}
                     (s/optional-key :named_links) {s/Keyword {s/Keyword s/Str}}
                     (s/optional-key :annotations) s/Any
                     })

(s/defschema NewEntity (assoc (dissoc Entity :_id :_rev :links) (s/optional-key :links) {s/Keyword [s/Str]}))


(s/defschema EntityList [Entity])


;;; --- Routes --- ;;;
(defapi app

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
 :termsOfServiceUrl "https://ovation.io/terms_of_service")

; Note: if you change context version, also change ovation-rest.util/version-path to match
(swaggered "top-level"
          (context "/api" []
                   (context "/v1" []
                            (context "/:resource" [resource]
                                     (GET* "/" request
                                           :return [Entity]
                                           :query-params [api-key :- String]
                                           :path-params [resource :- (s/enum "projects" "sources" "protocols")]
                                           :summary "Get Projects, Protocols and Top-level Sources"

                                           (ok (entity/index-resource api-key resource)))))))

(swaggered "entities"
          (context "/api" []
                   (context "/v1" []
                            (context "/entities" []
                                     (POST* "/" request
                                            :return [Entity]
                                            :query-params [api-key :- String]
                                            :body [new-dto NewEntity]
                                            :summary "Creates and returns an entity"
                                            (ok (entity/create-entity api-key new-dto)))
                                     (context "/:id" [id]
                                              (GET* "/" request
                                                    :return [Entity]
                                                    :query-params [api-key :- String]
                                                    :summary "Returns entity with :id"
                                                    (ok (entity/get-entity api-key id)))
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
                                                       (ok (entity/delete-entity api-key id)))
                                       (context "/annotations" []
                                          (GET* "/" request
                                            :query-params [api-key :- String]
                                            :summary "Returns all annotations entities associated with entity"
                                            (ok (entity/get-annotations api-key id)))
                                          (GET* "/:annotation-type/:user-id/:id" [annotation-type user-id id]
                                            :query-params [api-key :- String]
                                            :summary "Returns the annotation with :id of :annotation-type for :user-id"
                                            (ok (entity/get-annotations api-key id))))

                                       (context "/links/:rel" [rel]
                                          (GET* "/" request
                                            :return [Entity]
                                            :query-params [api-key :- String]
                                            :summary "Returns all entities associated with entity/rel"
                                            (ok (entity/get-link api-key id rel)))
                                          (POST* "/:target" [target]
                                            :return [Entity]
                                            :query-params [api-key :- String]
                                            :summary "Creates a new link to id :target with no inverse rel"
                                            (ok (entity/create-link api-key id rel target nil)))
                                          (POST* "/:target/:inverse" [target inverse]
                                            :return [Entity]
                                            :query-params [api-key :- String]
                                            :summary "Creates a new link to id :target with inverse rel :inverse"
                                            (ok (entity/create-link api-key id rel target inverse)))
                                          (DELETE* "/:target" target
                                            :return Success
                                            :query-params [api-key :- String]
                                            :summary "Deletes a link to id :target"
                                            (ok (entity/delete-link api-key id rel target))))

                                       (context "/named_links/:rel/:named" [rel named]
                                          (GET* "/" request
                                            :return [Entity]
                                            :query-params [api-key :- String]
                                            :summary "Returns all entities associated with entity/rel/named"
                                            (ok (entity/get-named-link api-key id rel named)))
                                          (POST* "/:target/" [target]
                                            :return [Entity]
                                            :query-params [api-key :- String]
                                            :summary "Creates a new named link to id :target with no inverse rel"
                                            (ok (entity/create-named-link api-key id rel named target nil)))
                                          (POST* "/:target/:inverse" [target inverse]
                                            :return [Entity]
                                            :query-params [api-key :- String]
                                            :summary "Creates a new named link to id :target with inverse rel :inverse"
                                            (ok (entity/create-named-link api-key id rel named target inverse)))
                                          (DELETE* "/:target" [target]
                                            :return Success
                                            :query-params [api-key :- String]
                                            :summary "Deletes a named link"
                                            (ok (entity/delete-named-link api-key id rel named target)))))))))

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

