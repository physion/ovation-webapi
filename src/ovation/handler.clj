(ns ovation.handler
  (:import (us.physion.ovation.domain OvationEntity$AnnotationKeys)
           (clojure.lang ExceptionInfo))
  (:require [compojure.api.sweet :refer :all]
            [clojure.string :refer [join]]
            [ring.util.http-response :refer [created ok accepted no-content not-found throw! unauthorized bad-request]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ovation.schema :refer :all]
            [ovation.logging]
            [ovation.config :as config]
            [ovation.annotations :as annotations]
            [ovation.core :as core]
            [slingshot.slingshot :refer [try+]]
            [ovation.middleware.token-auth :refer [wrap-token-auth]]
            [ovation.links :as links]
            [ovation.auth :as auth]
            [ring.middleware.conditional :refer [if-url-starts-with]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [clojure.string :refer [lower-case capitalize]]))

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


(defmacro get-resources
  "Get all resources of type (e.g. \"Project\")"
  [entity-type]
  (let [type-name (capitalize entity-type)
        type-path (lower-case (str type-name "s"))
        type-kw (keyword type-path)]
    `(GET* "/" request#
       :name ~(keyword (str "all-" (lower-case type-name)))
       :return {~type-kw [Entity]}
       :summary (str "Gets all top-level " ~type-path)
       (let [auth# (:auth/auth-info request#)
             entities# (core/of-type auth# ~type-name)]
         (ok {~type-kw entities#})))))

(defmacro post-resources
  "POST to resources of type (e.g. \"Project\")"
  [entity-type]
  (let [type-name (capitalize entity-type)
        type-path (lower-case (str type-name "s"))
        type-kw (keyword type-path)]
    `(POST* "/" request#
       :name ~(keyword (str "create-" (lower-case type-name)))
       :return {~type-kw [Entity]}
       :body [entities# [NewEntity]]
       :summary ~(str "Creates a new top-level " type-name)
       (let [auth# (:auth/auth-info request#)]
         (if (every? #(= ~type-name (:type %)) entities#)
           (try+
             (created {~type-kw (core/create-entity auth# entities#)})

             (catch [:type :ovation.auth/unauthorized] err#
               (unauthorized {})))

           (bad-request (str "Entities must be of \"type\" " ~type-name)))))))

(defmacro get-resource
  [entity-type id]
  (let [type-name (capitalize entity-type)
        single-type-kw (keyword (lower-case type-name))]
    `(GET* "/" request#
       :name ~(keyword (str "get-" (lower-case type-name)))
       :return {~single-type-kw Entity}
       :summary ~(str "Returns " type-name " with :id")
       (let [auth# (:auth/auth-info request#)]
         (if-let [entities# (core/get-entities auth# [~id])]
           (if-let [projects# (seq (filter #(= ~type-name (:type %)) entities#))]
             (ok {~single-type-kw (first projects#)})
             (not-found {})))))))

(defmacro post-resource
  [entity-type id]
  (let [type-name (capitalize entity-type)]
    `(POST* "/" request#
       :name ~(keyword (str "create-" (lower-case type-name) "-entity"))
       :return {:entities [Entity]}
       :body [entities# [NewEntity]]
       :summary ~(str "Creates and returns a new entity with the identified " type-name " as collaboration root")
       (let [auth# (:auth/auth-info request#)]
         (try+
           (created {:entities (core/create-entity auth# entities# :parent ~id)})

           (catch [:type :ovation.auth/unauthorized] err#
             (unauthorized {})))))))

(defmacro put-resource
  [entity-type id]
  (let [type-name (capitalize entity-type)
        type-path (lower-case (str type-name "s"))
        type-kw (keyword type-path)]
    `(PUT* "/" request#
       :name ~(keyword (str "update-" (lower-case type-name)))
       :return {~type-kw [Entity]}
       :body [update# EntityUpdate]
       :summary ~(str "Updates and returns " type-name " with :id")
       (let [entity-id# (str (:_id update#))]
         (if-not (= ~id (str entity-id#))
           (not-found {:error (str ~type-name " " entity-id# " ID mismatch")})
           (try+
             (let [auth# (:auth/auth-info request#)
                   entities# (core/update-entity auth# [update#])]
               (ok {~type-kw entities#}))

             (catch [:type :ovation.auth/unauthorized] err#
               (unauthorized {}))))))))

(defmacro delete-resource
  [entity-type id]
  (let [type-name (capitalize entity-type)]

    `(DELETE* "/" request#
       :name ~(keyword (str "delete-" (lower-case type-name)))
       :return {:entities [TrashedEntity]}
       :summary ~(str "Deletes (trashes) " type-name " with :id")
       (try+
         (let [auth# (:auth/auth-info request#)]
           (accepted {:entities (core/delete-entity auth# [~id])}))
         (catch [:type :ovation.auth/unauthorized] err#
           (unauthorized {}))))))



;;; --- Routes --- ;;;
(defapi app

  (middlewares [
                (wrap-cors
                  :access-control-allow-origin #".+"        ;; Allow from any origin
                  :access-control-allow-methods [:get :put :post :delete :options]
                  :access-control-allow-headers ["Content-Type" "Accept"])

                ;; Require authorization (via header token auth) for all paths starting with /api
                (wrap-token-auth
                  :authserver config/AUTH_SERVER
                  :required-auth-url-prefix #{"/api"})
                ]

    (swagger-ui)
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
             {:name "users" :description "Users"}
             {:name "analyses" :description "Analysis Records"}
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
              (let [auth (:auth/auth-info request)]
                (if-let [entities (core/get-entities auth [id])]
                  (ok {:entity (first entities)})
                  (not-found {}))))

            (POST* "/" request
              :name :create-entity
              :return {:entities [Entity]}
              :body [entities [NewEntity]]
              :summary "Creates and returns a new entity with the identified entity as collaboration root"
              (let [auth (:auth/auth-info request)]
                (try+
                  (created {:entities (core/create-entity auth entities :parent id)})

                  (catch [:type :ovation.auth/unauthorized] err
                    (unauthorized {})))))

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
                :return {:entities [Entity]
                         :links    [LinkInfo]}
                :body [links [NewEntityLink]]
                :summary "Adds a link"
                (try+
                  (let [auth (:auth/auth-info request)
                        sources (core/get-entities auth [id])
                        updates (flatten (for [src sources  ;; TODO this is pretty inefficient — can we make add-link take collections?
                                               link links]
                                           (links/add-link auth src rel (:target_id link))))]

                    (created {:entities (core/update-entity auth updates)
                              :links    (filter :rel updates)}))
                  (catch [:type :ovation.auth/unauthorized] err
                    (unauthorized {:error (:message err)}))))

              (context "/:target" [target]
                (DELETE* "/" request
                  :name :delete-links
                  :return {:links [LinkInfo]}
                  :summary "Remove links"
                  (try+
                    (let [auth (:auth/auth-info request)
                          user-id (auth/authenticated-user-id auth)
                          source (first (core/get-entities auth [id]))
                          update (links/delete-link auth source user-id rel target)]

                      (accepted {:links update}))
                    (catch [:type :ovation.auth/unauthorized] err
                      (unauthorized {:error (:message err)}))))))))

        (context* "/projects" []
          :tags ["projects"]
          (get-resources "Project")
          (post-resources "Project")
          (context* "/:id" [id]
            (get-resource "Project" id)
            (post-resource "Project" id)
            (put-resource "Project" id)
            (delete-resource "Project" id)))

        (context* "/sources" []
          :tags ["sources"]
          (get-resources "Source")
          (post-resources "Source")
          (context* "/:id" [id]
            (get-resource "Source" id)
            (post-resource "Source" id)                     ;; TODO only allow Source children
            (put-resource "Source" id)
            (delete-resource "Source" id)))

        (context* "/users" []
          :tags ["users"]
          (get-resources "User")
          (context* "/:id" [id]
            (get-resource "User" id)))

        (context* "/analysisrecords" []
          :tags ["analyses"]
          (get-resources "AnalysisRecord")
          (POST* "/" request
            :name :create-analysis
            :return {:analysis-records [Entity]}
            :body [analyses [NewAnalysisRecord]]
            :summary "Creates and returns a new Analysis Record"
            (let [auth (:auth/auth-info request)]
              (try+
                (let [analysis-records (map (fn [analysis]
                                              {:type       "AnalysisRecord"
                                               :attributes (if-let [params (:parameters analysis)]
                                                             {:parameters params}
                                                             {})}) analyses)]
                  (created {:analysis-records (core/create-entity auth analysis-records)})) ;;TODO

                (catch [:type :ovation.auth/unauthorized] err
                  (unauthorized {})))))
          (context* "/:id" [id]
            (get-resource "AnalysisRecord" id)
            (put-resource "AnalysisRecord" id)
            (delete-resource "AnalysisRecord" id)))))))


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

