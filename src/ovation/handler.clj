(ns ovation.handler
  (:import (us.physion.ovation.domain OvationEntity$AnnotationKeys)
           (clojure.lang ExceptionInfo))
  (:require [compojure.api.sweet :refer :all]
            [clojure.string :refer [join]]
            [ring.util.http-response :refer [created ok accepted no-content not-found throw! unauthorized]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ovation.schema :refer :all]
            [ovation.logging]
            [ovation.config :as config]
            [ovation.annotations :as annotations]
            [ovation.core :as core]
            [slingshot.slingshot :refer [try+]]
            [ovation.middleware.token-auth :refer [wrap-token-auth]]
            [ovation.links :as links]
            [ovation.auth :as auth]))

(ovation.logging/setup!)

(defmacro annotation
  "Creates an annotation type endpoint"
  [id annotation-type annotation-key record-schema annotation-schema]

  `(context* ~(str "/" annotation-type) []
     (GET* "/" []
       ;:return [~annotation-schema]
       :summary ~(str "Returns all " annotation-type " annotations associated with entity :id")
       (ok (entity/get-specific-annotations api-key# ~id ~annotation-key)))

     (POST* "/" []
       :return Success
       :body [new-annotation# ~record-schema]
       :summary ~(str "Adds a new " annotation-type " annotation to entity :id")
       (ok (entity/add-annotation api-key# ~id ~annotation-key new-annotation#)))

     (context* "/:annotation-id" [annotation-id#]
       (DELETE* "/" []
         :return Success
         :summary ~(str "Removes a " annotation-type " annotation from entity :id")
         (ok (entity/delete-annotation api-key# ~id ~annotation-key annotation-id#))))))



;;; --- Routes --- ;;;
(defapi app

  (middlewares [
                (wrap-cors
                  :access-control-allow-origin #".+"        ;; Allow from any origin
                  :access-control-allow-methods [:get :put :post :delete :options]
                  :access-control-allow-headers ["Content-Type" "Accept"])

                (wrap-token-auth
                  :authserver config/AUTH_SERVER
                  :skip-uris #{"/"})
                ]

    (swagger-ui
      )
    (swagger-docs
      {:info {
              :version        "1.0.0"
              :title          "Ovation"
              :description    "Ovation Web API"
              :contact        {:name  "Ovation"
                               :email "support@ovation.io"
                               :url   "https://support.ovation.io"}
              :termsOfService "https://ovation.io/terms_of_service"}}
      :tags [{:name "entities" :description "Generic entity operations"}
             {:name "projects" :description "Projects"}
             {:name "annotations" :description "Per-user annotations"}
             {:name "links" :description "Relationships between entities"}])


    (context* "/api" []
      (context* "/v1" []
        (context* "/entities" []
          :tags ["entities"]
          (context* "/:id" [id]

            (GET* "/" request
              :name :get-entity
              :return {:entity Entity}
              :summary "Returns entity with :id"
              (let [auth (:auth/auth-info request)
                    entity (first (core/get-entities auth [id]))]
                (if (nil? entity)
                  (not-found {})
                  (ok {:entity entity}))))

            (POST* "/" request
              :name :create-entity
              :return {:entities [Entity]}
              :body [entities [NewEntity]]
              :summary "Creates and returns a new entity with the identified entity as collaboration root"
              (let [auth (:auth/auth-info request)]
                (try+
                  (created {:entities (core/create-entity auth entities :parent id)})

                  (catch [:type :ovation.auth/unauthorized] err
                    (unauthorized {}))
                  )))

            (PUT* "/" request
              :name :update-entity
              :return {:entities [Entity]}
              :body [update EntityUpdate]
              :summary "Updates and returns entity with :id"
              (let [entity-id (str (:_id update))]
                (if-not (= id (str entity-id))
                  (not-found {:error (str "Entity " entity-id " ID mismatch")})
                  (try+
                    (let [auth (:auth/auth-info request)
                          entities (core/update-entity auth [update])]
                      (ok {:entities entities}))

                      (catch [:type :ovation.auth/unauthorized] err
                        (unauthorized {}))))))

            (DELETE* "/" request
              :name :delete-entity
              :return {:entities [TrashedEntity]}
              :summary "Deletes (trashes) entity with :id"
              (try+
                (let [auth (:auth/auth-info request)]
                  (accepted {:entities (core/delete-entity auth [id])}))
                (catch [:type :ovation.auth/unauthorized] err
                  (unauthorized {}))))

            (context* "/links/:rel" [rel]
              :tags ["links"]
              (GET* "/" request
                :name :get-links
                :return {:rel [Entity]}
                :summary "Gets the targets of relationship :rel from the identified entity"
                (let [auth (:auth/auth-info request)]
                  (ok {:rel (links/get-link-targets auth id rel)})))

              (POST* "/" request
                :name :create-links
                :return {:entities [Entity]}
                :body [links [NewEntityLink]]
                :summary "Adds a link"
                (try+
                  (let [auth (:auth/auth-info request)
                        user-id (auth/authorized-user-id auth)
                        sources (core/get-entities auth [id])
                        updates (flatten (for [src sources  ;; TODO is is pretty inefficient — can we make add-link take collections?
                                               link links]
                                           (links/add-link auth src user-id rel (:target_id link))))]

                    (created {:entities (core/update-entity auth updates)}))
                  (catch [:type :ovation.auth/unauthorized] err
                    (unauthorized {:error (:message err)}))))

              (context "/:target" [target]
                (DELETE* "/" request
                  :name :delete-links
                  :return {:links [LinkDoc]}
                  :summary "Remove links"
                  (try+
                    (let [auth (:auth/auth-info request)
                          user-id (auth/authorized-user-id auth)
                          source (first (core/get-entities auth [id]))
                          update (links/delete-link auth source user-id rel target)]

                      (accepted {:links update}))
                    (catch [:type :ovation.auth/unauthorized] err
                      (unauthorized {:error (:message err)}))))))

            ;(context* "/annotations" []
            ;  :tags ["annotations"]
            ;  (GET* "/" request
            ;    ;:return AnnotationsMap
            ;    :summary "Returns all annotations associated with entity"
            ;    (let [auth (:auth/api-key request)]
            ;      (ok (annotations/get-annotations auth id))))
            ;
            ;
            ;  (annotation id "keywords" OvationEntity$AnnotationKeys/TAGS TagRecord TagAnnotation)
            ;  (annotation id "properties" OvationEntity$AnnotationKeys/PROPERTIES PropertyRecord PropertyAnnotation)
            ;  (annotation id "timeline-events" OvationEntity$AnnotationKeys/TIMELINE_EVENTS TimelineEventRecord TimelineEventAnnotation)
            ;  (annotation id "notes" OvationEntity$AnnotationKeys/NOTES NoteRecord NoteAnnotation))
            )

          ;
          ;(context* "/links" []
          ;  (POST* "/" []
          ;    :return [Success]
          ;    :body [link Link]
          ;    :query-params [api-key :- s/Str]
          ;    :summary "Creates a new link to from this entity"
          ;    (created (links/create-link api-key id link)))
          ;
          ;  (context* "/:rel" [rel]
          ;    (GET* "/" []
          ;      :return [Entity]
          ;      :query-params [api-key :- s/Str]
          ;      :summary "Returns all entities associated with entity by the given relation"
          ;      (ok (links/get-link api-key id rel)))
          ;
          ;    (POST* "/" []
          ;      :return Success
          ;      :body [link NewEntityLink]
          ;      :query-params [api-key :- s/Str]
          ;      :summary "Creates a new link"
          ;      (created (links/create-link api-key id (assoc link :rel rel))))
          ;
          ;    (DELETE* "/:target" [target]
          ;      :return Success
          ;      :query-params [api-key :- s/Str]
          ;      :summary "Deletes a link to the given target (uuid)"
          ;      (ok (links/delete-link api-key id rel target)))))
          ;
          ;(context* "/named_links" []
          ;  (POST* "/" request
          ;    :return [Entity]
          ;    :body [link NamedLink]
          ;    :query-params [api-key :- s/Str]
          ;    :summary "Creates a new named link to id :target with no inverse rel"
          ;    (created (links/create-named-link api-key id link)))
          ;
          ;  (context* "/:rel/:named" [rel named]
          ;    (GET* "/" request
          ;      :return [Entity]
          ;      :query-params [api-key :- s/Str]
          ;      :summary "Returns all entities associated with entity by the given relation and name"
          ;      (ok (links/get-named-link api-key id rel named)))
          ;    (POST* "/" request
          ;      :return Success
          ;      :body [link NewEntityLink]
          ;      :query-params [api-key :- s/Str]
          ;      :summary "Creates a new named link"
          ;      (created (links/create-named-link api-key id (assoc link :name named :rel rel))))
          ;    (DELETE* "/:target" [target]
          ;      :return Success
          ;      :query-params [api-key :- s/Str]
          ;      :summary "Deletes a named link to the given target (uuid)"
          ;      (ok (links/delete-named-link api-key id rel named target)))))
          )))))

