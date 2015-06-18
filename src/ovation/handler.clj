(ns ovation.handler
  (:require [compojure.api.sweet :refer :all]
            [compojure.api.routes :refer [path-for*]]
            [ring.util.http-response :refer [created ok accepted not-found unauthorized bad-request]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ovation.schema :refer :all]
            [ovation.logging]
            [clojure.tools.logging :as logging]
            [ovation.config :as config]
            [ovation.annotations :as annotations]
            [ovation.core :as core]
            [slingshot.slingshot :refer [try+ throw+]]
            [ovation.middleware.token-auth :refer [wrap-token-auth]]
            [ovation.links :as links]
            [ovation.auth :as auth]
            [ovation.analyses :refer [create-analysis-record ANALYSIS_RECORD_TYPE]]
            [ring.middleware.conditional :refer [if-url-starts-with]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [clojure.string :refer [lower-case capitalize join]]
            [schema.core :as s]))

(ovation.logging/setup!)

(defn router
  [request]
  (fn [name]
    (path-for* name request)))

(defmacro annotation
  "Creates an annotation type endpoint"
  [id annotation-description annotation-key record-schema annotation-schema]

  `(context* ~(str "/" annotation-key) []
     :tags [~annotation-key]
     (GET* "/" request#
       :name ~(keyword (str "get-" (lower-case annotation-key)))
       ;:return {s/Keyword [~annotation-schema]}
       :summary ~(str "Returns all " annotation-description " annotations associated with entity :id")
       (let [auth# (:auth/auth-info request#)
             annotations# (annotations/get-annotations auth# [~id] ~annotation-key)]
         (ok {(keyword ~annotation-key) annotations#})))

     (POST* "/" request#
       :name ~(keyword (str "create-" (lower-case annotation-key)))
       :return {s/Keyword [~annotation-schema]}
       :body [new-annotations# [~record-schema]]
       :summary ~(str "Adds a new " annotation-description " annotation to entity :id")
       (let [auth# (:auth/auth-info request#)]
         (created {(keyword ~annotation-key) (annotations/create-annotations auth# [~id] ~annotation-key new-annotations#)})))

     (context* "/:annotation-id" [aid#]
       (DELETE* "/" request#
         :name ~(keyword (str "delete-" (lower-case annotation-key)))
         :return [s/Str]
         :summary ~(str "Removes a " annotation-description " annotation from entity :id")
         (let [auth# (:auth/auth-info request#)
               annotation-id# (-> request# :route-params :annotation-id)]
           (accepted (map :_id (annotations/delete-annotations auth# [annotation-id#]))))))))


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


                (wrap-with-logger                           ;;TODO can we make the middleware conditional rather than testing for each logging call?
                  :info (fn [x] (when config/LOGGING_HOST (logging/info x)))
                  :debug (fn [x] (when config/LOGGING_HOST (logging/debug x)))
                  :error (fn [x] (when config/LOGGING_HOST (logging/error x)))
                  :warn (fn [x] (when config/LOGGING_HOST (logging/warn x))))
                ]

    (swagger-ui)
    (swagger-docs
      {:info {
              :version        "1.0.0"
              :title          "Ovation"
              :description    "Ovation Web API"
              :contact        {:name  "Ovation"
                               :url   "https://ovation.io"}
              :termsOfService "https://ovation.io/terms_of_service"}}
      :tags [{:name "entities" :description "Generic entity operations"}
             {:name "projects" :description "Projects"}
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
            (delete-resource "Project" id)))

        (context* "/experiments" []
          :tags ["experiments"]
          (get-resources "Experiment")
          (post-resources "Experiment")
          (context* "/:id" [id]
            (get-resource "Experiment" id)
            (post-resource "Experiment" id)
            (put-resource "Experiment" id)
            (delete-resource "Experiment" id)))

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

