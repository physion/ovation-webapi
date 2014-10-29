(ns ovation-rest.handler
  (:import (us.physion.ovation.domain OvationEntity$AnnotationKeys))
  (:require [clojure.string :refer [join]]
            [ring.util.http-response :refer [created ok accepted]]
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
            [ovation-rest.schema :refer :all]
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
                (ok (util/into-seq (conj () (util/get-entity api-key id)))))
              (PUT* "/" request
                :return [Entity]
                :query-params [api-key :- s/Str]
                :body [dto Entity]
                :summary "Updates and returns updated entity with :id"
                (ok (entity/update-entity-attributes api-key id (:attributes dto))))
              (DELETE* "/" request
                :return Success
                :query-params [api-key :- s/Str]
                :summary "Deletes entity with :id"
                (accepted (entity/delete-entity api-key id)))

              (context "/links" []
                (POST* "/" []
                  :return [Entity]
                  :body [link Link]
                  :query-params [api-key :- s/Str]
                  :summary "Creates a new link to from this entity"
                  (created (links/create-link api-key id link)))

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
                    (ok (links/delete-link api-key id rel target)))))

              (context "/named_links" []
                (POST* "/" request
                  :return [Entity]
                  :body [link NamedLink]
                  :query-params [api-key :- s/Str]
                  :summary "Creates a new named link to id :target with no inverse rel"
                  (created (links/create-named-link api-key id link)))

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
                    (ok (links/delete-named-link api-key id rel named target))))))))))

    (swaggered "annotations"
      (context "/api" []
        (context "/v1" []
          (context "/entities" []
            (context "/:id" [id]
              (context "/annotations" []

                (context "/:annotation-id" [annotation-id]
                  (DELETE* "/" request
                  :return Success
                  :query-params [api-key :- String]
                  (ok (entity/delete-annotation api-key id annotation-id))))

                (GET* "/" request
                  :return Success
                  :query-params [api-key :- String]
                  :summary "Returns all annotations associated with entity"
                  (ok (entity/get-annotations api-key id)))
                (POST* "/tags" request
                  :return Success
                  :query-params [api-key :- String]
                  :body [new-annotation TagRecord]
                  :summary "Adds a new annotation (owned by current authenticated user) to this entity"
                  (ok (entity/add-annotation api-key id OvationEntity$AnnotationKeys/TAGS new-annotation)))
                (POST* "/properties" request
                  :return Success
                  :query-params [api-key :- String]
                  :body [new-annotation PropertyRecord]
                  :summary "Adds a new annotation (owned by current authenticated user) to this entity"
                  (ok (entity/add-annotation api-key id OvationEntity$AnnotationKeys/PROPERTIES new-annotation)))
                (POST* "/timeline-events" request
                  :return Success
                  :query-params [api-key :- String]
                  :body [new-annotation TimelineEventRecord]
                  :summary "Adds a new annotation (owned by current authenticated user) to this entity"
                  (ok (entity/add-annotation api-key id OvationEntity$AnnotationKeys/TIMELINE_EVENTS new-annotation)))
                (POST* "/notes" request
                  :return Success
                  :query-params [api-key :- String]
                  :body [new-annotation NoteRecord]
                  :summary "Adds a new annotation (owned by current authenticated user) to this entity"
                  (ok (entity/add-annotation api-key id OvationEntity$AnnotationKeys/NOTES new-annotation)))))))))

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

