(ns ovation-rest.handler
  (:import (us.physion.ovation.domain OvationEntity$AnnotationKeys))
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
                     (s/optional-key :annotations) {s/Keyword {s/Keyword {s/Keyword #{{s/Keyword s/Str}}}}}
                     })

(s/defschema NewEntity (assoc (dissoc Entity :_id :_rev :links) (s/optional-key :links) {s/Keyword [s/Str]}))




(def AnnotationBase {:_id    s/Str
                     :_rev   s/Str
                     :user   s/Str
                     :entity s/Str})

(s/defschema AnnotationTypes (s/enum OvationEntity$AnnotationKeys/TAGS
                               OvationEntity$AnnotationKeys/PROPERTIES
                               OvationEntity$AnnotationKeys/NOTES
                               OvationEntity$AnnotationKeys/TIMELINE_EVENTS))

(s/defschema TagRecord {:tag s/Str})
(s/defschema TagAnnotation (conj AnnotationBase {:annotation_type OvationEntity$AnnotationKeys/TAGS
                                                 :annotation      TagRecord}))


(s/defschema PropertyRecord {:key   s/Str
                             :value (describe s/Str "(may be any JSON type)")})
(s/defschema PropertyAnnotation (conj AnnotationBase {:annotation_type OvationEntity$AnnotationKeys/PROPERTIES
                                                      :annotation      PropertyRecord}))


(s/defschema NoteRecord {:text      s/Str
                         :timestamp s/Str})
(s/defschema NoteAnnotation (conj AnnotationBase {:annotation_type OvationEntity$AnnotationKeys/NOTES
                                                  :annotation      NoteRecord}))


(s/defschema TimelineEventRecord {:name  s/Str
                                  :notes s/Str
                                  :start s/Str
                                  (s/optional-key :end) s/Str})
(s/defschema TimelineEventAnnotation (conj AnnotationBase {:annotation_type OvationEntity$AnnotationKeys/TIMELINE_EVENTS
                                                           :annotation      TimelineEventRecord}))



(s/defschema NewAnnotation (describe (s/either TagRecord PropertyRecord NoteRecord TimelineEventRecord) "A new annotation record"))
(s/defschema Annotation (describe (s/either TagAnnotation PropertyAnnotation NoteAnnotation TimelineEventAnnotation) "An annotation"))




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

    (swaggered "entities"
      (context "/api" []
        (context "/v1" []
          (context "/:resource" [resource]
            (GET* "/" request
              :return [Entity]
              :query-params [api-key :- String]
              :path-params [resource :- (s/enum "projects" "sources" "protocols")]
              :summary "Get Projects, Protocols and Top-level Sources"

              (ok (entity/index-resource api-key resource))))

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
              ;                                              (PUT* "/" request
              ;                                                    :return [Entity]
              ;                                                    :query-params [api-key :- String]
              ;                                                    :body [dto Entity]
              ;                                                    :summary "Updates and returns updated entity with :id"
              ;                                                    (ok (entity/update-entity api-key id dto (util/host-context request :remove-levels 1))))
              (DELETE* "/" request
                :return Success
                :query-params [api-key :- String]
                :summary "Deletes entity with :id"
                (ok (entity/delete-entity api-key id)))
              (context "/links/:rel" [rel]
                (GET* "/" request
                  :return [Entity]
                  :query-params [api-key :- String]
                  :summary "Returns all entities associated with entity/rel"
                  (ok (entity/get-entity-link api-key id rel))))
              (context "/named_links/:rel/:named" [rel named]
                (GET* "/" request
                  :return [Entity]
                  :query-params [api-key :- String]
                  :summary "Returns all entities associated with entity/rel/named"
                  (ok (entity/get-entity-named-link api-key id rel named)))))))))

    (swaggered "annotations"
      (context "/api" []
        (context "/v1" []
          (context "/entities" []
            (context "/annotations" []
              (context "/keywords" []
                (POST* "/" request
                  :return TagAnnotation
                  :query-params [api-key :- String]
                  :body [new-annotation TagRecord]
                  :summary "Adds a new annotation (owned by current authenticated user) to this entity"
                  (ok "ANNOTATIONS!")))
              (context "/properties" []
                (POST* "/" request
                  :return PropertyAnnotation
                  :query-params [api-key :- String]
                  :body [new-annotation PropertyRecord]
                  :summary "Adds a new annotation (owned by current authenticated user) to this entity"
                  (ok "ANNOTATIONS!")))
              (context "/timeline_events" []
                (POST* "/" request
                  :return TimelineEventAnnotation
                  :query-params [api-key :- String]
                  :body [new-annotation TimelineEventRecord]
                  :summary "Adds a new annotation (owned by current authenticated user) to this entity"
                  (ok "ANNOTATIONS!")))
              (context "/notes" []
                (POST* "/" request
                  :return NoteAnnotation
                  :query-params [api-key :- String]
                  :body [new-annotation NoteRecord]
                  :summary "Adds a new annotation (owned by current authenticated user) to this entity"
                  (ok "ANNOTATIONS!"))))))))

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

