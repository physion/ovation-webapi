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
            [ovation-rest.links :as links]
            [ovation-rest.util :as util]
            [ovation-rest.schema :refer [Success Entity NewEntity Link NamedLink]]
            ))




;;; --- Routes --- ;;;
(defapi app

  (middlewares [(wrap-cors
                  :access-control-allow-origin #".+"        ; FIXME - accept only what we want here
                  :access-control-allow-methods [:get :put :post :delete :options]
                  :access-control-allow-headers ["Content-Type" "Accept"])]
    (swagger-ui)
    (swagger-docs
      :apiVersion "1.0.0"
      :title "Ovation"
      :description "Ovation Web API"
      :contact "support@ovation.io"
      :termsOfServiceUrl "https://ovation.io/terms_of_service")

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
              :query-params [api-key :- s/Str]
              :body [new-dto NewEntity]
              :summary "Creates and returns an entity"
              (created (entity/create-entity api-key new-dto)))
            (context "/:id" [id]
              (GET* "/" request
                :return [Entity]
                :query-params [api-key :- s/Str]
                :summary "Returns entity with :id"
                (ok (conj [] (entity/get-entity api-key id))))
              (PUT* "/" request
                :return [Entity]
                :query-params [api-key :- s/Str]
                :body [dto Entity]
                :summary "Updates and returns updated entity with :id"
                (ok (entity/update-entity api-key id dto (util/host-context request :remove-levels 1))))
              (DELETE* "/" request
                :return Success
                :query-params [api-key :- s/Str]
                :summary "Deletes entity with :id"
                (ok (entity/delete-entity api-key id)))
              (context "/annotations" []
                (GET* "/" request
                  :query-params [api-key :- s/Str]
                  :summary "Returns all annotations entities associated with entity"
                  (ok (entity/get-annotations api-key id)))
                (GET* "/:annotation-type/:user-id/:id" [annotation-type user-id id]
                  :query-params [api-key :- s/Str]
                  :summary "Returns the annotation with :id of :annotation-type for :user-id"
                  (ok (entity/get-annotations api-key id))))

              (context "/links" []
                (context "/:rel" [rel]
                  (GET* "/" []
                    :return [Entity]
                    :query-params [api-key :- s/Str]
                    :summary "Returns all entities associated with entity by the given relation"
                    (ok (links/get-link api-key id rel)))

                  (DELETE* "/:target" [target]
                    :return Success
                    :query-params [api-key :- s/Str]
                    :summary "Deletes a link to the given target (uuid)"
                    (ok (links/delete-link api-key id rel target))))

                (POST* "/" []
                   :return [Entity]
                   :body [link Link]
                   :query-params [api-key :- s/Str]
                   :summary "Creates a new link to from this entity"
                   (created (links/create-link api-key id link))))

              (context "/named_links" []
                (context "/:rel/:named" [rel named]
                  (GET* "/" request
                    :return [Entity]
                    :query-params [api-key :- s/Str]
                    :summary "Returns all entities associated with entity by the given relation and name"
                    (ok (links/get-named-link api-key id rel named)))
                  (DELETE* "/:target" [target]
                    :return Success
                    :query-params [api-key :- s/Str]
                    :summary "Deletes a named link to the given target (uuid)"
                    (ok (links/delete-named-link api-key id rel named target))))

                (POST* "/" request
                  :return [Entity]
                  :body [link NamedLink]
                  :query-params [api-key :- s/Str]
                  :summary "Creates a new named link to id :target with no inverse rel"
                  (created (links/create-named-link api-key id link)))
                ))))))

    (swaggered "views"
      (context "/api" []
        (context "/v1" []
          (context "/views" []
            (GET* "/*" request
              :return [Entity]
              :query-params [api-key :- s/Str]
              :summary "Returns entities in view. Views follow CouchDB calling conventions (http://wiki.apache.org/couchdb/HTTP_view_API)"
              (let [host (util/host-from-request request)]
                (ok (entity/get-view api-key
                      (url-normalize (format "%s/%s?%s" host (:uri request) (util/ovation-query request)))
                      (util/host-context request :remove-levels 1)))))))))))

