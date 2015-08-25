(ns ovation.handler
  (:require [compojure.api.sweet :refer :all]
            [compojure.api.routes :refer [path-for*]]
            [ring.util.http-response :refer [created ok accepted not-found unauthorized bad-request]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ovation.schema :refer :all]
            [ovation.logging]
            [ovation.route-helpers :refer [router annotation get-resources post-resources get-resource post-resource put-resource delete-resource]]
            [clojure.tools.logging :as logging]
            [ovation.config :as config]
            [ovation.core :as core]
            [slingshot.slingshot :refer [try+ throw+]]
            [ovation.middleware.token-auth :refer [wrap-token-auth]]
            [ovation.links :as links]
            [ovation.auth :as auth]
            [ovation.analyses :refer [create-analysis-record ANALYSIS_RECORD_TYPE]]
            [ring.middleware.conditional :refer [if-url-starts-with]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [ring.middleware.raygun :refer [wrap-raygun-handler]]
            [clojure.string :refer [lower-case capitalize join]]
            [schema.core :as s]))

(ovation.logging/setup!)





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


                (wrap-with-logger                           ;;TODO can we make the middleware conditional rather than testing for each logging call?
                  :info (fn [x] (when config/LOGGING_HOST (logging/info x)))
                  :debug (fn [x] (when config/LOGGING_HOST (logging/debug x)))
                  :error (fn [x] (when config/LOGGING_HOST (logging/error x)))
                  :warn (fn [x] (when config/LOGGING_HOST (logging/warn x))))

                (wrap-raygun-handler (System/getenv "RAYGUN_API_KEY"))
                ]

    (swagger-ui)
    (swagger-docs
      {:info {
              :version        "2.0.0"
              :title          "Ovation"
              :description    "Ovation Web API"
              :contact        {:name  "Ovation"
                               :url   "https://ovation.io"}
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
              :summary "Returns entity with :id"
              (let [auth (:auth/auth-info request)]
                (if-let [entities (core/get-entities auth [id])]
                  (ok {:entity (first entities)})
                  (not-found {}))))

                    ;; TODO Remove POST in favor of entity-specific (with schema validation)
            (POST* "/" request
              :name :create-entity
              :return {:entities [Entity]}
              :body [entities [NewEntity]]
              :summary "Creates and returns a new entity with the identified entity as collaboration root"
              (let [auth (:auth/auth-info request)]
                (try+
                  (created {:entities (core/create-entity auth entities :parent id)})

                  (catch [:type :ovation.auth/unauthorized] err
                    (unauthorized {:error (:type err)})))))

                    ;; TODO Remove PUT in favor of entity-specific (with schema validation)
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
                      (unauthorized {:error (:type err)}))))))

            (DELETE* "/" request
              :name :delete-entity
              :return {:entities [TrashedEntity]}
              :summary "Deletes (trashes) entity with :id"
              (try+
                (let [auth (:auth/auth-info request)]
                  (accepted {:entities (core/delete-entity auth [id])}))
                (catch [:type :ovation.auth/unauthorized] err
                  (unauthorized {:error (:type err)}))))

            (context* "/annotations" []
              :tags ["annotations"]
              (annotation id "keywords" "tags" TagRecord TagAnnotation)
              (annotation id "properties" "properties" PropertyRecord PropertyAnnotation)
              (annotation id "timeline events" "timeline_events" TimelineEventRecord TimelineEventAnnotation)
              (annotation id "notes" "notes" NoteRecord NoteAnnotation))

            (context* "/links/:rel" [rel]
              :tags ["links"]
              (GET* "/" request
                :name :get-links
                :return {s/Keyword [Entity]}
                :summary "Gets the targets of relationship :rel from the identified entity"
                (let [auth (:auth/auth-info request)]
                  (ok {(keyword rel) (links/get-link-targets auth id rel)})))

              (POST* "/" request
                :name :create-links
                :return {:entities [Entity]
                         :links    [LinkInfo]}
                :body [new-links [NewEntityLink]]
                :summary "Adds a link"
                (try+
                  (let [auth (:auth/auth-info request)
                        source (first (core/get-entities auth [id]))]
                    (if source
                      (let [all-updates (:all (links/add-links auth source rel (map :target_id new-links)))
                            updates (core/update-entity auth all-updates :direct true)]
                        (created {:entities (filter :type updates)
                                  :links    (filter :rel updates)}))
                      (not-found {:error (str id " not found")})))
                  (catch [:type :ovation.auth/unauthorized] {:keys [message]}
                    (unauthorized {:error message}))
                  (catch [:type :ovation.links/target-not-found] {:keys [message]}
                    (bad-request {:error message}))))

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
                    (catch [:type :ovation.auth/unauthorized] {:keys [message]}
                      (unauthorized {:error message}))))))))

        (context* "/projects" []
          :tags ["projects"]
          (get-resources "Project")
          (post-resources "Project")
          (context* "/:id" [id]
            (get-resource "Project" id)
            (post-resource "Project" id)
            (put-resource "Project" id)
            (delete-resource "Project" id)
            (context* "/links/:rel" [rel]
              :tags ["links"]
              (GET* "/" request
                    :name :get-project-links
                    :return {s/Keyword [Entity]}
                    :summary "Gets the targets of relationship :rel from the identified entity"
                    (let [auth (:auth/auth-info request)]
                      (ok {(keyword rel) (links/get-link-targets auth id rel)})))

              (POST* "/" request
                     :name :create-project-links
                     :return {:entities [Entity]
                              :links    [LinkInfo]}
                     :body [new-links [NewEntityLink]]
                     :summary "Adds a link"
                     (try+
                       (let [auth (:auth/auth-info request)
                             source (first (core/get-entities auth [id]))]
                         (if source
                           (let [all-updates (:all (links/add-links auth source rel (map :target_id new-links)))
                                 updates (core/update-entity auth all-updates :direct true)]
                             (created {:entities (filter :type updates)
                                       :links    (filter :rel updates)}))
                           (not-found {:error (str id " not found")})))
                       (catch [:type :ovation.auth/unauthorized] {:keys [message]}
                         (unauthorized {:error message}))
                       (catch [:type :ovation.links/target-not-found] {:keys [message]}
                         (bad-request {:error message}))))

              (context "/:target" [target]
                       (DELETE* "/" request
                                        :name :delete-project-links
                                        :return {:links [LinkInfo]}
                                        :summary "Remove links"
                                        (try+
                                          (let [auth (:auth/auth-info request)
                                                user-id (auth/authenticated-user-id auth)
                                                source (first (core/get-entities auth [id]))
                                                update (links/delete-link auth source user-id rel target)]

                                            (accepted {:links update}))
                                          (catch [:type :ovation.auth/unauthorized] {:keys [message]}
                                            (unauthorized {:error message}))))))))


        (context* "/sources" []
          :tags ["sources"]
          (get-resources "Source")
          (post-resources "Source")
          (context* "/:id" [id]
            (get-resource "Source" id)
                    (post-resource "Source" id)                     ;; TODO only allow Source children; pass list of Schema, or base if empty
            (put-resource "Source" id)
            (delete-resource "Source" id)))


                (context* "/folders" []
                          :tags ["folders"]
                          (get-resources "Folder")
                          (post-resources "Folder")
                          (context* "/:id" [id]
                                    (get-resource "Folder" id)
                                    (post-resource "Folder" id)                     ;; TODO only allow Folder or Resource/Revision children; pass list of Schema, or base if empty
                                    (put-resource "Folder" id)
                                    (delete-resource "Folder" id)))




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
                (let [records (doall (map #(create-analysis-record auth %) analyses))] ;;TODO could we create all the records at once?
                  (created {:analysis-records (concat records)}))

                (catch [:type ::links/target-not-found] {:keys [message]}
                  (bad-request {:error message}))
                (catch [:type ::links/illegal-target-type] {:keys [message]}
                  (bad-request {:error message})))))
          (context* "/:id" [id]
            (get-resource "AnalysisRecord" id)
            (put-resource "AnalysisRecord" id)
            (delete-resource "AnalysisRecord" id)))


        (context* "/provenance" []
          :tags ["provenance"]
          (POST* "/" request
            :name :get-provenance
            ;:return {:provenance ProvGraph}
            :summary "Returns the provenance graph expanding from the POSTed entity IDs"
            (let [auth (:auth/auth-info request)]
              nil)))))))

