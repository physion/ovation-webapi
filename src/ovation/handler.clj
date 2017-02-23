(ns ovation.handler
  (:require [compojure.api.sweet :refer :all]
            [compojure.api.routes :refer [path-for*]]
            [compojure.route :as route]
            [ring.util.http-response :refer [created ok no-content accepted not-found unauthorized bad-request conflict temporary-redirect]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.logger :refer [wrap-with-logger]]
            [ovation.middleware.raygun :refer [wrap-raygun-handler]]
            [slingshot.slingshot :refer [try+ throw+]]
            [clojure.string :refer [lower-case capitalize join]]
            [ovation.schema :refer :all]
            [ovation.routes :refer [router]]
            [ovation.route-helpers :refer [annotation get-resources post-resources get-resource post-resource put-resource delete-resource rel-related relationships post-revisions* get-head-revisions* move-contents*]]
            [ovation.config :as config]
            [ovation.core :as core]
            [ovation.middleware.auth :refer [wrap-authenticated-teams]]
            [ovation.links :as links]
            [ovation.routes :as r]
            [ovation.auth :as auth]
            [ovation.audit]
            [ovation.search :as search]
            [ovation.breadcrumbs :as breadcrumbs]
            [schema.core :as s]
            [ovation.teams :as teams]
            [new-reliquary.ring :refer [wrap-newrelic-transaction]]
            [ovation.prov :as prov]
            [buddy.auth.backends.token :refer [jws-backend]]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [buddy.auth :refer [authenticated?]]
            [buddy.auth.accessrules :refer [wrap-access-rules]]
            [ring.logger :as ring.logger]
            [clojure.java.io :as io]
            [ovation.revisions :as revisions]
            [ovation.util :as util]
            [ovation.routes :as routes]))


(def rules [{:pattern #"^/api.*"
             :handler authenticated?}])

(def DESCRIPTION "")
;(slurp (io/file (io/resource "description.md")))

;;; --- Routes --- ;;;
(defroutes static-resources
  (route/resources "/public"))

(defn create-app [database]
  (let [db (:connection database)]
    (api
      {:swagger {:ui   "/"
                 :spec "/swagger.json"
                 :data {:info {
                               :version        "2.0.0"
                               :title          "Ovation"
                               :description    DESCRIPTION
                               :contact        {:name "Ovation"
                                                :url  "https://www.ovation.io"}
                               :termsOfService "https://services.ovation.io/terms_of_service"}
                        :tags [{:name "entities" :description "Generic entity operations"}
                               {:name "activities" :description "Describe inputs and outputs of a procedure"}
                               {:name "projects" :description "Manage Projects"}
                               {:name "folders" :description "Mange Folders"}
                               {:name "files" :description "Manage Files"}
                               {:name "sources" :description "Manage Sources, the subject of measurements"}
                               {:name "users" :description "Get information about Users"}
                               {:name "annotations" :description "Per-user annotations"}
                               {:name "links" :description "Relationships between entities"}
                               {:name "provenance" :description "Provenance graph"}
                               {:name "teams" :description "Manage collaborations"}
                               {:name "ui" :description "Support for Web UI"}
                               {:name "search" :description "Search Ovation"}
                               {:name "zip" :description "Download ZIP archive"}]}}}


      (middleware [[wrap-cors
                    :access-control-allow-origin #".+"      ;; Allow from any origin
                    :access-control-allow-methods [:get :put :post :delete :options]
                    :access-control-allow-headers [:accept :content-type :authorization :origin]]

                   [wrap-authentication (jws-backend {:secret     config/JWT_SECRET
                                                      :token-name "Bearer"})]
                   [wrap-access-rules {:rules    rules
                                       :on-error auth/unauthorized-response}]

                   wrap-authenticated-teams

                   [ring.logger/wrap-with-logger {:printer :identity-printer}]

                   [wrap-raygun-handler (config/config "RAYGUN_API_KEY")]

                   wrap-newrelic-transaction]



                  (undocumented
                    static-resources)

        (context "/api" []
          (context "/v1" []
            (context "/entities" []
              :tags ["entities"]
              (context "/:id" []
                :path-params [id :- s/Str]
                (GET "/" request
                  :name :get-entity
                  :query-params [{trash :- (s/maybe s/Bool) false}]
                  :return {:entity TrashedEntity}
                  :responses {404 {:schema JsonApiError :description "Not found"}}
                  :summary "Returns entity with :id. If include-trashed is true, result includes entity even if it's in the trash."
                  (let [auth (auth/identity request)]
                    (if-let [entities (core/get-entities auth [id] (router request) :include-trashed trash)]
                      (if-let [entity (first entities)]
                        (ok {:entity entity})
                        (not-found {:errors {:detail "Not found"}}))
                      (not-found {:errors {:detail "Not found"}}))))
                (DELETE "/" request
                  :name :delete-entity
                  :return {:entity TrashedEntity}
                  :summary "Deletes entity with :id. Deleted entities can be restored."
                  (try+
                    (let [auth (auth/identity request)]
                      (accepted {:entity (first (core/delete-entities auth db [id] (r/router request)))}))
                    (catch [:type :ovation.auth/unauthorized] err
                      (unauthorized {:errors {:detail "Delete not authorized"}}))))
                (PUT "/restore" request
                  :name :restore-entity
                  :return {:entity Entity}
                  :body [body {:entity TrashedEntityUpdate}]
                  :summary "Restores a deleted entity from the trash."
                  (try+
                    (let [auth (auth/identity request)]
                      (ok {:entity (first (core/restore-deleted-entities auth db [id] (r/router request)))}))
                    (catch [:type :ovation.auth/unauthorized] err
                      (unauthorized {:errors {:detail "Restore` not authorized"}}))))

                (context "/annotations" []
                  :tags ["annotations"]
                  :name :annotations
                  (annotation db id "keywords" "tags" TagRecord TagAnnotation)
                  (annotation db id "properties" "properties" PropertyRecord PropertyAnnotation)
                  (annotation db id "timeline events" "timeline_events" TimelineEventRecord TimelineEventAnnotation)
                  (annotation db id "notes" "notes" NoteRecord NoteAnnotation))))

            (context "/relationships" []
              :tags ["links"]
              (context "/:id" []
                :path-params [id :- s/Str]
                (GET "/" request
                  :name :get-relation
                  :return {:relationship LinkInfo}
                  :summary "Relationship document"
                  (let [auth (auth/identity request)]
                    (ok {:relationship (first (core/get-values auth [id] :routes (r/router request)))})))
                (DELETE "/" request
                  :name :delete-relation
                  :return {:relationship LinkInfo}
                  :summary "Removes relationship"
                  (let [auth         (auth/identity request)
                        relationship (first (core/get-values auth db [id]))]
                    (if relationship
                      (let [source (first (core/get-entities auth db [(:source_id relationship)] (r/router request)))]
                        (accepted {:relationships (links/delete-links db auth (r/router request)
                                                    source
                                                    (:_id relationship))}))
                      (not-found {:errors {:detail "Not found"}}))))))

            (context "/projects" []
              :tags ["projects"]
              (get-resources db "Project")
              (post-resources db "Project" [NewProject])
              (context "/:id" []
                :path-params [id :- s/Str]

                (get-resource db "Project" id)
                (post-resource db "Project" id [NewFolder NewFile NewChildActivity])
                (put-resource db "Project" id)
                (delete-resource db "Project" id)

                (context "/links/:rel" []
                  :path-params [rel :- s/Str]

                  (rel-related db "Project" id rel)
                  (relationships db "Project" id rel))))


            (context "/sources" []
              :tags ["sources"]
              (get-resources db "Source")
              (post-resources db "Source" [NewSource])
              (context "/:id" []
                :path-params [id :- s/Str]

                (get-resource db "Source" id)
                (post-resource db "Source" id [NewSource])
                (put-resource db "Source" id)
                (delete-resource db "Source" id)

                (context "/links/:rel" []
                  :path-params [rel :- s/Str]

                  (rel-related db "Source" id rel)
                  (relationships db "Source" id rel))))


            (context "/activities" []
              :tags ["activities"]
              (get-resources db "Activity")
              (post-resources db "Activity" [NewActivity])
              (context "/:id" []
                :path-params [id :- s/Str]

                (get-resource db "Activity" id)
                (put-resource db "Activity" id)
                (delete-resource db "Activity" id)

                (context "/links/:rel" []
                  :path-params [rel :- s/Str]

                  (rel-related db "Activity" id rel)
                  (relationships db "Activity" id rel))))

            (context "/folders" []
              :tags ["folders"]
              (get-resources db "Folder")
              (context "/:id" []
                :path-params [id :- s/Str]

                (get-resource db "Folder" id)
                (post-resource db "Folder" id [NewFolder NewFile])
                (put-resource db "Folder" id)
                (delete-resource db "Folder" id)
                (POST "/move" request
                  :name :move-folder
                  :return {s/Keyword (s/either File Folder)
                           :links    [{s/Keyword s/Any}]
                           :updates  [{s/Keyword s/Any}]}
                  :summary "Move folder from source folder to destination folder"
                  :body [info {:source      s/Str
                               :destination s/Str}]
                  (created (routes/self-route2 (routes/router request) "folder" id)
                    (move-contents* request db id info)))

                (context "/links/:rel" []
                  :path-params [rel :- s/Str]

                  (rel-related db "Folder" id rel)
                  (relationships db "Folder" id rel))))


            (context "/files" []
              :tags ["files"]
              (get-resources db "File")
              (context "/:id" []
                :path-params [id :- s/Str]

                (get-resource db "File" id)
                (POST "/" request
                  :name :create-file-entity
                  :return CreateRevisionResponse
                  :body [revisions {:entities [NewRevision]}]
                  :summary "Creates a new downstream Revision from the current HEAD Revision"
                  (created (routes/heads-route2 (routes/router request) id)
                    (post-revisions* request db id (:entities revisions))))

                (POST "/move" request
                  :name :move-file
                  :return {s/Keyword (s/either File Folder)
                           :links    [{s/Keyword s/Any}]
                           :updates  [{s/Keyword s/Any}]}
                  :summary "Move file from source folder to destination folder"
                  :body [info {:source      s/Str
                               :destination s/Str}]
                  (created (routes/self-route2 (routes/router request) "file" id)
                    (move-contents* request db id info)))

                (GET "/heads" request
                  :name :file-head-revisions
                  :return {:revisions [Revision]}
                  :summary "Gets the HEAD revision(s) for this file"
                  (get-head-revisions* request db id))
                (put-resource db "File" id)
                (delete-resource db "File" id)

                (context "/links/:rel" []
                  :path-params [rel :- s/Str]

                  (rel-related db "File" id rel)
                  (relationships db "File" id rel))))


            (context "/revisions" []
              :tags ["files"]
              (context "/:id" []
                :path-params [id :- s/Str]

                (get-resource db "Revision" id)
                (put-resource db "Revision" id)
                (delete-resource db "Revision" id)
                (POST "/" request
                  :name :create-revision-entity
                  :return CreateRevisionResponse
                  :body [revisions [NewRevision]]
                  :summary "Creates a new downstream Revision"
                  (created (routes/targets-route2 (routes/router request) "revision" id "revisions")
                    (post-revisions* request db id revisions)))
                (PUT "/upload-complete" request
                  :name :upload-complete
                  :summary "Indicates upload is complete and updates metadata from S3 for this Revision"
                  :return {:revision Revision}
                  (let [auth     (auth/identity request)
                        rt       (router request)
                        revision (core/get-entity auth id rt)]
                    (ok {:revision (revisions/update-metadata auth db rt revision)})))
                (PUT "/upload-failed" request
                  :name :upload-failed
                  :summary "Indicates upload failed and updates the File status"
                  :return {:revision Revision
                           :file     File}
                  (let [auth     (auth/identity request)
                        rt       (router request)
                        revision (first (core/get-entities auth db [id] rt))
                        result   (revisions/record-upload-failure auth db rt revision)]
                    (ok {:revision (:revision result)
                         :file     (:file result)})))
                (context "/links/:rel" []
                  :path-params [rel :- s/Str]

                  (rel-related db "Revision" id rel)
                  (relationships db "Revision" id rel))))



            (context "/prov" []
              :tags ["provenance"]
              (context "/:id" []
                :path-params [id :- s/Str]

                (GET "/" request
                  :name :entity-provenance
                  :return {:provenance [{:_id      s/Uuid
                                         :type     s/Str
                                         :name     s/Str
                                         s/Keyword [{:_id s/Uuid :name s/Str :type s/Str}]}]}
                  :summary "Local provenance for a single entity"
                  (let [auth   (auth/identity request)
                        rt     (router request)
                        result (prov/local auth db rt [id])]
                    (ok {:provenance result})))))

            (context "/teams" []
              :tags ["teams"]

              (context "/:id" []
                :path-params [id :- s/Str]

                (GET "/" request
                  :name :get-team
                  :return {:team             Team
                           :users            [TeamUser],
                           :membership_roles [TeamMembershipRole]}
                  :summary "Gets Project Team"
                  (ok (teams/get-team* request id)))
                (context "/memberships" []
                  (POST "/" request
                    :name :post-memberships
                    :return {s/Keyword (s/either TeamMembership PendingTeamMembership)}
                    :summary "Creates a new team membership (adding a user to a team). Returns the created membership. May return a pending membership if the user is not already an Ovation user. Upon signup an invited user will be added as a team member."
                    :body [body {:membership NewTeamMembershipRole}]
                    (let [membership (teams/post-membership* request id (:membership body))]
                      (created ((routes/router request) :get-team {:id id}) membership)))
                  (context "/:mid" []
                    :path-params [mid :- s/Str]

                    (PUT "/" request
                      :name :put-membership
                      :summary "Updates an existing membership by setting its role."
                      :return {:membership TeamMembership}
                      :body [body {:membership NewTeamMembershipRole}]
                      (ok (teams/put-membership* request id (:membership body) mid)))

                    (DELETE "/" request
                      :name :delete-membership
                      :summary "Deletes a team membership, removing the team member."
                      (teams/delete-membership* request mid)
                      (no-content))))
                (context "/pending" []
                  (context "/:mid" []
                    :path-params [mid :- s/Str]

                    (PUT "/" request
                      :name :put-pending-membership
                      :summary "Updates a pending membership by setting its role."
                      :return {:membership PendingTeamMembership}
                      :body [body {:membership NewTeamMembershipRole}]
                      (ok (teams/put-pending-membership* request id (:membership body) mid)))

                    (DELETE "/" request
                      :name :delete-pending-membership
                      :summary "Deletes a pending membership. Upon signup, the user will no longer become a team member."
                      (teams/delete-pending-membership* request mid)
                      (no-content))))))

            (context "/roles" []
              :tags ["teams"]

              (GET "/" request
                :name :all-roles
                :return {:roles [TeamRole]}
                :summary "Gets all team Roles for the current Organization"
                (ok (teams/get-roles* request))))

            (context "/breadcrumbs" []
              :tags ["ui"]
              (GET "/" request
                :query-params [id :- s/Str]
                :name :get-breadcrumbs
                :return {:breadcrumbs [[{:type s/Str :id s/Uuid :name s/Str}]]}
                :summary "Gets the breadcrumbs for an entity."
                (let [auth   (auth/identity request)
                      rt     (router request)
                      result (breadcrumbs/get-breadcrumbs auth db rt [id])]
                  (ok {:breadcrumbs (get result id)})))

              (POST "/" request
                :body [ids [s/Str]]
                :return {:breadcrumbs {s/Uuid [[{:type s/Str :id s/Uuid :name s/Str}]]}}
                :summary "Gets the breadcrumbs for a collection of entities. Allows POSTing for large collections"
                (let [auth   (auth/identity request)
                      rt     (router request)
                      result (breadcrumbs/get-breadcrumbs auth db rt ids)]
                  (ok {:breadcrumbs result}))))

            (context "/zip" []
              :tags ["zip"]
              (context "/folders" []
                (GET "/:id" request
                  :path-params [id :- s/Uuid]
                  :name :zip-folder
                  :summary "Download Folder contents as a Zip archive"
                  (temporary-redirect (util/join-path [config/ZIP_SERVICE "api" "v1" "folders" id]))))
              (context "/activities" []
                (GET "/:id" request
                  :path-params [id :- s/Uuid]
                  :name :zip-activity
                  :summary "Download Activity contents as a Zip archive"
                  (temporary-redirect (util/join-path [config/ZIP_SERVICE "api" "v1" "activities" id])))))

            (context "/search" []
              :tags ["search"]
              (GET "/" request
                :query-params [q :- s/Str
                               {bookmark :- (s/maybe s/Str) nil}
                               {limit :- s/Int 25}]
                :summary "Searches the Ovation database"
                :return {:search_results [{:id            s/Uuid
                                           :entity_type   s/Str
                                           :name          s/Str
                                           :owner         s/Uuid
                                           :updated-at    (s/maybe s/Str) ;; allow nil updated-at
                                           :project_names [s/Str]
                                           :links         {:breadcrumbs s/Str}}]
                         :meta           {:bookmark   s/Str
                                          :total_rows s/Int}}
                (let [auth   (auth/identity request)
                      rt     (router request)
                      result (search/search auth rt q :bookmark bookmark :limit limit)]
                  (ok result))))))))))
