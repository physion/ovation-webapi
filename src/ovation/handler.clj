(ns ovation.handler
  (:require [compojure.api.sweet :refer :all]
            [compojure.api.routes :refer [path-for*]]
            [ring.util.http-response :refer [created ok no-content accepted not-found unauthorized bad-request conflict]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.logger :refer [wrap-with-logger]]
            [ring.middleware.raygun :refer [wrap-raygun-handler]]
            [slingshot.slingshot :refer [try+ throw+]]
            [clojure.string :refer [lower-case capitalize join]]
            [ovation.schema :refer :all]
            [ovation.logging :as logging]
            [ovation.routes :refer [router]]
            [ovation.route-helpers :refer [annotation get-resources post-resources get-resource post-resource put-resource delete-resource rel-related relationships post-revisions* get-head-revisions* move-contents*]]
            [ovation.config :as config]
            [ovation.core :as core]
            [ovation.middleware.auth :refer [wrap-authenticated-teams wrap-log-identity]]
            [ovation.links :as links]
            [ovation.routes :as r]
            [ovation.auth :as auth]
            [schema.core :as s]
            [ovation.teams :as teams]
            [new-reliquary.ring :refer [wrap-newrelic-transaction]]
            [ovation.prov :as prov]
            [buddy.auth.backends.token :refer (jws-backend)]
            [buddy.auth.middleware :refer (wrap-authentication)]
            [buddy.auth :refer [authenticated?]]
            [buddy.auth.accessrules :refer [wrap-access-rules]]
            [ring.logger.timbre :as logger.timbre]))


(ovation.logging/setup!)

(def rules [{:pattern #"^/api.*"
             :handler authenticated?}])

;;; --- Routes --- ;;;
(defapi app

  (middlewares [
                (wrap-cors
                  :access-control-allow-origin #".+"        ;; Allow from any origin
                  :access-control-allow-methods [:get :put :post :delete :options]
                  :access-control-allow-headers [:accept :content-type :authorization :origin])

                (wrap-authentication (jws-backend {:secret config/JWT_SECRET
                                                   :token-name "Bearer"}))
                (wrap-access-rules {:rules rules
                                    :on-error auth/throw-unauthorized})

                (wrap-authenticated-teams)

                (logger.timbre/wrap-with-logger)

                (wrap-log-identity)

                (wrap-raygun-handler (System/getenv "RAYGUN_API_KEY"))

                (wrap-newrelic-transaction)]


    (swagger-ui)
    (swagger-docs
      {:info {
              :version        "2.0.0"
              :title          "Ovation"
              :description    "Ovation Web API"
              :contact        {:name "Ovation"
                               :url  "https://ovation.io"}
              :termsOfService "https://ovation.io/terms_of_service"}}
      :tags [{:name "entities" :description "Generic entity operations"}
             {:name "projects" :description "Projects"}
             {:name "folders" :description "Folders"}
             {:name "files" :description "Files"}
             {:name "protocols" :description "Protocols"}
             {:name "sources" :description "Sources"}
             {:name "users" :description "Users"}
             {:name "analyses" :description "Analysis Records"}
             {:name "annotations" :description "Per-user annotations"}
             {:name "links" :description "Relationships between entities"}
             {:name "provenance" :description "Provenance graph"}])


    (context* "/api" []
      (context* "/v1" []
        (context* "/entities" []
          :tags ["entities"]
          (context* "/:id" [id]

            (GET* "/" request
              :name :get-entity
              :return {:entity Entity}
              :responses {404 {:schema JsonApiError :description "Not found"}}
              :summary "Returns entity with :id"
              (let [auth (auth/identity request)]
                (if-let [entities (core/get-entities auth [id] (router request))]
                  (ok {:entity (first entities)})
                  (not-found {:errors {:detail "Not found"}}))))

            (context* "/annotations" []
              :tags ["annotations"]
              :name :annotations
              (annotation id "keywords" "tags" TagRecord TagAnnotation)
              (annotation id "properties" "properties" PropertyRecord PropertyAnnotation)
              (annotation id "timeline events" "timeline_events" TimelineEventRecord TimelineEventAnnotation)
              (annotation id "notes" "notes" NoteRecord NoteAnnotation))))

        (context* "/relationships" []
          :tags ["links"]
          (context* "/:id" [id]
            (GET* "/" request
              :name :get-relation
              :return {:relationship LinkInfo}
              :summary "Relationship document"
              (let [auth (auth/identity request)]
                (ok {:relationship (first (core/get-values auth [id] :routes (r/router request)))})))
            (DELETE* "/" request
              :name :delete-relation
              :return {:relationship LinkInfo}
              :summary "Removes relationship"
              (let [auth (auth/identity request)
                    relationship (first (core/get-values auth [id]))]
                (if relationship
                  (let [source (first (core/get-entities auth [(:source_id relationship)] (r/router request)))]
                    (accepted {:relationships (links/delete-links auth (r/router request)
                                                source
                                                (:_id relationship))}))
                  (not-found {:errors {:detail "Not found"}}))))))

        (context* "/projects" []
          :tags ["projects"]
          (get-resources "Project")
          (post-resources "Project" [NewProject])
          (context* "/:id" [id]
            (get-resource "Project" id)
            (post-resource "Project" id [NewFolder NewFile NewActivity])
            (put-resource "Project" id)
            (delete-resource "Project" id)

            (context* "/links/:rel" [rel]
              (rel-related "Project" id rel)
              (relationships "Project" id rel))))


        (context* "/sources" []
          :tags ["sources"]
          (get-resources "Source")
          (post-resources "Source" [NewSource])
          (context* "/:id" [id]
            (get-resource "Source" id)
            (post-resource "Source" id [NewSource])
            (put-resource "Source" id)
            (delete-resource "Source" id)

            (context* "/links/:rel" [rel]
              (rel-related "Source" id rel)
              (relationships "Source" id rel))))


        (context* "/activities" []
          :tags ["activities"]
          (get-resources "Activity")
          (context* "/:id" [id]
            (get-resource "Activity" id)
            (put-resource "Activity" id)
            (delete-resource "Activity" id)

            (context* "/links/:rel" [rel]
              (rel-related "Activity" id rel)
              (relationships "Activity" id rel))))

        (context* "/folders" []
          :tags ["folders"]
          (get-resources "Folder")
          (context* "/:id" [id]
            (get-resource "Folder" id)
            (post-resource "Folder" id [NewFolder NewFile])
            (put-resource "Folder" id)
            (delete-resource "Folder" id)
            (POST* "/move" request
              :name :move-folder
              :return {s/Keyword (s/either File Folder)
                       :links [{s/Keyword s/Any}]
                       :updates [{s/Keyword s/Any}]}
              :summary "Move folder from source folder to destination folder"
              :body [info {:source s/Str
                           :destination s/Str}]
              (created (move-contents* request id info)))

            (context* "/links/:rel" [rel]
              (rel-related "Folder" id rel)
              (relationships "Folder" id rel))))


        (context* "/files" []
          :tags ["files"]
          (get-resources "File")
          (context* "/:id" [id]
            (get-resource "File" id)
            (POST* "/" request
              :name :create-file-entity
              :return CreateRevisionResponse
              :body   [revisions {:entities [NewRevision]}]
              :summary "Creates a new downstream Revision from the current HEAD Revision"
              (created (post-revisions* request id (:entities revisions))))

            (POST* "/move" request
              :name :move-file
              :return {s/Keyword (s/either File Folder)
                       :links [{s/Keyword s/Any}]
                       :updates [{s/Keyword s/Any}]}
              :summary "Move file from source folder to destination folder"
              :body [info {:source s/Str
                           :destination s/Str}]
              (created (move-contents* request id info)))

            (GET* "/heads" request
              :name :file-head-revisions
              :return {:revisions [Revision]}
              :summary "Gets the HEAD revision(s) for this file"
              (get-head-revisions* request id))
            (put-resource "File" id)
            (delete-resource "File" id)

            (context* "/links/:rel" [rel]
              (rel-related "File" id rel)
              (relationships "File" id rel))))


        (context* "/revisions" []
          :tags ["files"]
          (context* "/:id" [id]
            (get-resource "Revision" id)
            (put-resource "Revision" id)
            (delete-resource "Revision" id)
            (POST* "/" request
              :name :create-revision-entity
              :return CreateRevisionResponse
              :body [revisions [NewRevision]]
              :summary "Creates a new downstream Revision"
              (created (post-revisions* request id revisions)))
            (context* "/links/:rel" [rel]
              (rel-related "Revision" id rel)
              (relationships "Revision" id rel))))


        (context* "/prov" []
          :tags ["provenance"]
          (context* "/:id" [id]
            (GET* "/" request
              :name :entity-provenance
              :return {:provenance [{:_id s/Uuid
                                     :type s/Str
                                     :name s/Str
                                     s/Keyword [{:_id s/Uuid :name s/Str :type s/Str}]}]}
              :summary "Local provenance for a single entity"
              (let [auth   (auth/identity request)
                    rt     (router request)
                    result (prov/local auth rt [id])]
                (ok {:provenance result})))))

        (context* "/users" []
          :tags ["users"]
          (get-resources "User")
          (context* "/:id" [id]
            (get-resource "User" id)))

        (context* "/teams" []
          :tags ["teams"]

          (context* "/:id" [id]
            (GET* "/" request
              :name :get-team
              :return {:team Team
                       :users [TeamUser],
                       :membership_roles [TeamMembershipRole]}
              :summary "Gets Project Team"
              (ok (teams/get-team* request id)))
            (context* "/memberships" []
              (POST* "/" request
                :name :post-memberships
                :return {s/Keyword (s/either TeamMembership PendingTeamMembership)}
                :summary "Creates a new team Membership. Returns the created :membership. May return a :pending_membership if the user is not already an Ovation user."
                :body [body {:membership NewTeamMembership}]
                (created (teams/post-membership* request id (:membership body))))
              (context* "/:mid" [mid]
                (PUT* "/" request
                  :name :put-membership
                  :return {:membership TeamMembership}
                  :body [body {:membership NewTeamMembership}]
                  (ok (teams/put-membership* request id (:membership body) mid)))

                (DELETE* "/" request
                  :name :delete-membership
                  (teams/delete-membership* request mid)
                  (no-content))))))

        (context* "/roles" []
          :tags ["teams"]

          (GET* "/" request
            :name :all-roles
            :return {:roles [TeamRole]}
            :summary "Gets all team Roles for the current Organization"
            (ok (teams/get-roles* request))))))))
