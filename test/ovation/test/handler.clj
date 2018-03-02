(ns ovation.test.handler
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [ovation.handler :refer [create-app]]
            [clojure.data.json :as json]
            [slingshot.slingshot :refer [try+]]
            [ovation.auth :as auth]
            [ovation.core :as core]
            [clojure.walk :as walk]
            [ovation.util :as util]
            [ovation.version :as ver]
            [ovation.schema :refer [EntityChildren]]
            [ovation.links :as links]
            [clojure.string :refer [lower-case capitalize]]
            [ovation.annotations :as annotations]
            [ovation.routes :as r]
            [ovation.revisions :as revisions]
            [ovation.teams :as teams]
            [ovation.prov :as prov]
            [ovation.config :as config]
            [ovation.route-helpers :as rh]
            [buddy.sign.jwt :as jwt]
            [ovation.constants :as c]
            [ovation.breadcrumbs :as b]
            [ovation.routes :as routes]
            [ovation.transform.serialize :as serialize]
            [compojure.api.validator]
            [ovation.test.system :as test.system]
            [ovation.test.helpers :refer [sling-throwable]]
            [ovation.request-context :as request-context])
  (:import (java.util UUID)))

(def id {:uuid (UUID/randomUUID)})

(def TOKEN (jwt/sign id (config/config :jwt-secret)))

(def ORGS "o")

(def TEAMS (promise))
(deliver TEAMS [])

(def PERMISSIONS (promise))
(deliver PERMISSIONS {})


(defn mock-req
  [req apikey]
  (-> req
    (mock/header "Authorization" (format "Bearer %s" apikey))
    (mock/content-type "application/json")))


(defn json-post-body
  [m]
  (json/write-str (walk/stringify-keys m)))


(defn get*
  [app path apikey]
  (let [get      (mock-req (mock/request :get path) apikey)
        response (app get)
        reader   (clojure.java.io/reader (:body response))
        body     (json/read reader)]
    {:status (:status response)
     :body   (walk/keywordize-keys body)}))


(defn delete*
  [app path apikey]
  (let [get      (mock-req (mock/request :delete path) apikey)
        response (app get)
        reader   (clojure.java.io/reader (:body response))
        body     (json/read reader)]
    {:status (:status response)
     :body   (walk/keywordize-keys body)}))


(defn post*
  [app path apikey body]
  (let [post     (mock-req (-> (mock/request :post path)
                             (mock/body (json-post-body body))) apikey)
        response (app post)
        reader   (clojure.java.io/reader (:body response))
        body     (json/read reader)]

    {:status (:status response)
     :body   (walk/keywordize-keys body)}))


(defn put*
  [app path apikey body]
  (let [post     (mock-req (-> (mock/request :put path)
                             (mock/body (json-post-body body))) apikey)
        response (app post)

        reader   (clojure.java.io/reader (:body response))
        body     (json/read reader)]

    {:status (:status response)
     :body   (walk/keywordize-keys body)}))


(defn typepath
  [typename]
  (case (lower-case typename)
    "activity" "activities"
    ;;default
    (lower-case (str typename "s"))))

(defmacro entity-resources-read-tests
  "Facts about reading resources"
  [app db org entity-type]
  (let [type-name (capitalize entity-type)
        type-path (typepath type-name)]
    `(let [apikey# TOKEN]
       (against-background [(teams/get-teams anything) => TEAMS
                            (auth/get-permissions anything) => PERMISSIONS
                            (request-context/make-context anything ~org anything anything) => ..ctx..
                            ..ctx.. =contains=> {::request-context/routes ..rt..}]
         (facts ~(util/join-path ["" type-path])
           (facts "resources"
             (let [id#     (str (UUID/randomUUID))
                   entity# {:_id           id#
                            :type          ~type-name
                            :attributes    {}
                            :links         {:self "self"}
                            :relationships {}}]
               (let [get-req# (mock-req (mock/request :get (util/join-path ["" "api" ~ver/version ~ORGS ~org ~type-path])) apikey#)]
                 (against-background [(core/of-type ..ctx.. ~db ~type-name) => [entity#]]
                   (fact ~(str "GET / gets all " type-path)
                     (body-json ~app get-req#) => {~(keyword type-path) [entity#]}))))))))))


(defmacro entity-resource-read-tests
  "Facts about reading resource"
  [app db org entity-type]
  (let [type-name (capitalize entity-type)
        type-path (typepath type-name)]
    `(let [apikey# TOKEN]

       (against-background [(teams/get-teams anything) => TEAMS
                            (auth/get-permissions anything) => PERMISSIONS]
         (facts ~(util/join-path ["" type-path])
           (facts "resource"
             (let [id#     (str (UUID/randomUUID))
                   entity# {:_id           id#
                            :type          ~type-name
                            :attributes    {}
                            :links         {:self "self"}
                            :relationships {}}]
               (let [get-req# (mock-req (mock/request :get (util/join-path ["" "api" ~ver/version ~ORGS ~org ~type-path id#])) apikey#)]
                 (against-background [(request-context/make-context anything ~org anything anything)  => ..ctx..
                                      ..ctx.. =contains=> {::request-context/routes ..rt..}
                                      (core/get-entities ..ctx.. ~db [id#]) => [entity#]]
                   (fact ~(str "GET /:id gets a single " (lower-case type-name))
                     (body-json ~app get-req#) => {~(keyword (lower-case type-name)) entity#})
                   (let [source# {:_id        id#
                                  :type       "OtherType"
                                  :attributes {}
                                  :links      {:self ""}}]
                     (fact ~(str "GET /:id returns 404 if not a " (lower-case type-name))
                       (:status (~app get-req#)) => 404
                       (provided
                         (core/get-entities ..ctx.. ~db [id#]) => [source#]))))))))))))


(defmacro entity-resource-create-tests
  "Facts about a resource creation (e.g. \"Project\")"
  [app db org entity-type]

  (let [type-name (capitalize entity-type)
        type-path (typepath type-name)]
    `(let [apikey# TOKEN]
       (against-background [(teams/get-teams anything) => TEAMS
                            (auth/get-permissions anything) => PERMISSIONS
                            (teams/create-team ..ctx.. anything anything) => ..team..
                            (auth/identity anything) => ..auth..
                            (request-context/make-context anything ~org anything anything) => ..ctx..
                            ..ctx.. =contains=> {::request-context/routes ..rt..}]
         (facts ~(util/join-path ["" type-path])
           (facts "resource"
             (let [source-type#  ~(util/entity-type-name-keyword type-name)
                   target-type#  (key (first (source-type# EntityChildren)))
                   rel#          (get-in EntityChildren [source-type# target-type# :rel])
                   inverse_rel#  (get-in EntityChildren [source-type# target-type# :inverse-rel])

                   parent#       {:_id  ~(str (UUID/randomUUID))
                                  :type ~type-name}
                   new-entity#   {:type       (capitalize (name target-type#))
                                  :attributes {:foo "bar"}}
                   new-entities# {:entities [new-entity#]}
                   entity#       (assoc new-entity# :_id ~(str (UUID/randomUUID))
                                                    :_rev "1")

                   request#      (fn [] (mock-req (-> (mock/request :post (util/join-path ["" "api" ~ver/version ~ORGS ~org ~type-path (:_id parent#)]))
                                                    (mock/body (json/write-str (walk/stringify-keys new-entities#)))) apikey#))
                   links#        [{:type "Relation" :foo "bar"}]]


               (against-background [(core/create-entities ..ctx.. ~db [new-entity#] :parent (:_id parent#)) => [entity#]
                                    (core/get-entities ..ctx.. ~db [(:_id parent#)]) => [parent#]
                                    (links/add-links ..ctx.. ~db [parent#] rel# [(:_id entity#)] :inverse-rel inverse_rel#) => {:links links#}
                                    (core/create-values ..ctx.. ~db links#) => links#
                                    (core/update-entities ..ctx.. ~db anything :authorize false :update-collaboration-roots true) => ..updates..
                                    (request-context/router anything) => ..rt..]
                 (fact "POST /:id returns status 201"
                   (let [post# (request#)]
                     (:status (~app post#)) => 201))
                 (fact ~(str "POST /:id inserts entity with " type-name " parent")
                   (let [post# (request#)]
                     (body-json ~app post#) => {:entities [entity#]
                                                :links    links#
                                                :updates  []})))

               (fact "POST /:id returns 401 if not can? :create"
                 (:status (~app (request#))) => 401
                 (provided
                   (core/create-entities ..ctx.. ~db [new-entity#] :parent (:_id parent#)) =throws=> (sling-throwable {:type :ovation.auth/unauthorized}))))))))))


(defmacro entity-resources-create-tests
  "Facts about a resource creation (e.g. \"Project\")"
  [app db org entity-type]

  (let [type-name (capitalize entity-type)
        type-path (typepath type-name)]
    `(let [apikey# TOKEN]

       (against-background [(teams/get-teams anything) => TEAMS
                            (auth/get-permissions anything) => PERMISSIONS
                            (teams/create-team ..ctx.. anything anything) => ..team..
                            (auth/identity anything) => ..auth..
                            (request-context/make-context anything ~org anything anything) => ..ctx..
                            ..ctx.. =contains=> {::request-context/routes ..rt..}]
         (facts ~(util/join-path ["" type-path])
           (facts "create"
             (let [new-entity#         {:type ~(capitalize type-name) :attributes {:foo "bar"}}
                   plural-source-type# ~(util/entity-type-name-keyword type-path)

                   new-entities#       {plural-source-type# [new-entity#]}
                   entity#             (assoc new-entity# :_id ~(str (UUID/randomUUID))
                                                          :_rev "1")
                   request#            (fn [] (mock-req (-> (mock/request :post (util/join-path ["" "api" ~ver/version ~ORGS ~org ~type-path]))
                                                          (mock/body (json/write-str (walk/stringify-keys new-entities#)))) apikey#))]

               (against-background [(core/create-entities ..ctx.. ~db [new-entity#]) => [entity#]
                                    (core/create-values ..ctx.. ~db []) => []
                                    (core/update-entities ..ctx.. ~db [] :authorize false :update-collaboration-roots true) => []
                                    (request-context/router anything) => ..rt..]
                 (fact "POST / returns status 201"
                   (let [post# (request#)]
                     (:status (~app post#)) => 201))
                 (fact ~(str "POST / inserts new top-level " type-name)
                   (let [post# (request#)]
                     (body-json ~app post#) => {~(keyword type-path) [entity#]})))


               (let [bad-entities# [{:type "Other" :attributes {:foo "bar"}}]
                     bad-request#  (fn [] (mock-req (-> (mock/request :post (util/join-path ["" "api" ~ver/version ~ORGS ~org ~type-path]))
                                                      (mock/body (json/write-str (walk/stringify-keys bad-entities#)))) apikey#))]
                 (fact "POST / returns 400 if type does not match"
                   (:status (~app (bad-request#))) => 400))

               (fact "POST / returns 401 if not can? :create"
                 (:status (~app (request#))) => 401
                 (provided
                   (core/create-entities ..ctx.. ~db [new-entity#]) =throws=> (sling-throwable {:type :ovation.auth/unauthorized}))))))))))


(defmacro entity-resource-update-tests
  "Facts about a resource update (e.g. \"Project\")"
  [app db org entity-type]
  (let [type-name (capitalize entity-type)
        type-path (typepath type-name)]
    `(let [apikey# TOKEN]

       (against-background [(teams/get-teams anything) => TEAMS
                            (auth/get-permissions anything) => PERMISSIONS
                            (auth/identity anything) => ..auth..
                            (request-context/make-context anything ~org anything anything) => ..ctx..
                            ..ctx.. =contains=> {::request-context/routes ..rt..}]
         (facts ~(util/join-path ["" type-path])
           (facts "update"
             (let [id#             (UUID/randomUUID)
                   attributes#     {:foo "bar"}
                   entity#         {:type          ~type-name
                                    :_id           id#
                                    :attributes    attributes#
                                    :links         {:self "self"}
                                    :relationships {}}
                   new-attributes# {:bar "baz"}
                   update#         (-> entity#
                                     (dissoc :links)
                                     (dissoc :relationships)
                                     (assoc :attributes new-attributes#))
                   put-body#       {~(util/entity-type-name-keyword type-name) (assoc update# :_id (str id#))}
                   updated-entity# (assoc update# :links {:self "self"} :relationships {} :_id (str id#))
                   request#        (fn [entity-id#] (mock-req (-> (mock/request :put (util/join-path ["" "api" ~ver/version ~ORGS ~org ~type-path (str entity-id#)]))
                                                                (mock/body (json/write-str (walk/stringify-keys put-body#)))) apikey#))]

               (against-background [(core/update-entities ..ctx.. ~db [update#]) => [updated-entity#]
                                    (request-context/router anything) => ..rt..]
                 (fact "succeeds with status 200"
                   (let [response# (~app (request# id#))]
                     (:status response#) => 200))
                 (fact "updates single entity by ID"
                   (let [request# (request# id#)]
                     (body-json ~app request#)) => {~(util/entity-type-name-keyword type-name) updated-entity#})
                 (fact "fails if entity and path :id do not match"
                   (let [other-id# (str (UUID/randomUUID))
                         response# (~app (request# other-id#))]
                     (:status response#) => 404)))

               ;(against-background [(request-context/router anything) => ..rt..]
               ;  (fact "fails with status 409"
               ;    (let [response# (app (request# id#))]
               ;      (:status response#) => 409
               ;      (provided
               ;        (core/update-entities ..ctx.. ~db [update#]) =throws=> (sling-throwable {:status 409})))))

               (fact "fails if not can? :update"
                 (:status (~app (request# id#))) => 401
                 (provided
                   (core/update-entities ..ctx.. ~db [update#]) =throws=> (sling-throwable {:type :ovation.auth/unauthorized}))))))))))


(defmacro entity-resource-deletion-tests
  "Facts about a resource type (e.g. \"Project\")"
  [app db org entity-type]
  (let [type-name (capitalize entity-type)
        type-path (typepath type-name)]
    `(let [apikey# TOKEN]

       (against-background [(teams/get-teams anything) => TEAMS
                            (auth/get-permissions anything) => PERMISSIONS
                            (auth/identity anything) => ..auth..
                            (request-context/make-context anything ~org anything anything) => ..ctx..
                            ..ctx.. =contains=> {::request-context/routes ..rt..}]

         (facts ~(util/join-path ["" type-path])
           (facts "delete"
             (let [id#             (UUID/randomUUID)
                   attributes#     {:foo "bar"}
                   entity#         {:type       "MyEntity"
                                    :_id        (str id#)
                                    :attributes attributes#
                                    :links      {:self "self"}}
                   deleted-entity# (assoc entity# :transh_info {:trashing_user (str (UUID/randomUUID))
                                                                :trasing_date  "1971-12-01"
                                                                :trash_root    ""})
                   request#        (fn [entity-id#] (mock-req (-> (mock/request :delete (util/join-path ["" "api" ~ver/version ~ORGS ~org ~type-path (str entity-id#)]))) apikey#))]

               (against-background [(core/delete-entities ..ctx.. ~db [(str id#)]) => [deleted-entity#]
                                    (request-context/router anything) => ..rt..]
                 (fact "succeeds with status 202"
                   (let [response# (~app (request# id#))]
                     (:status response#) => 202))
                 (fact "DELETE /:id trashes entity"
                   (let [delete-request# (request# id#)]
                     (body-json ~app delete-request#)) => {:entity deleted-entity#}))

               (fact "returns 401 if not can? :delete"
                 (:status (~app (request# id#))) => 401
                 (provided
                   (core/delete-entities ..ctx.. ~db [(str id#)]) =throws=> (sling-throwable {:type :ovation.auth/unauthorized}))))))))))

(defn body-json
  [app request]
  (let [response (app request)
        reader   (clojure.java.io/reader (:body response))
        result   (json/read reader)]
    (walk/keywordize-keys result)))


(against-background [(around :contents (test.system/system-background ?form))]
  (let [app (test.system/get-app)
        db  (test.system/get-db)
        org 1]



    (facts "About authorization"
      (fact "invalid API key returns 401"
        (let [apikey "12345"
              path   (util/join-path ["" "api" ver/version ORGS org "entities" "123"])
              get    (mock-req (mock/request :get path) apikey)]
          (:status (app get)) => 401)))


    (facts "About doc route"
      (let [apikey TOKEN]
        (fact "HEAD / => 302"
          (let [response (app (mock-req (mock/request :head "/") apikey))]
            (:status response) => 302))
        (fact "GET / redirects"
          (let [response (app (mock-req (mock/request :get "/") apikey))]
            (:status response) => 302
            (-> response (:headers) (get "Location")) => "/index.html"))
        (fact "HEAD /index.html returns 200"
          (let [response (app (mock-req (mock/request :head "/index.html") apikey))]
            (:status response) => 200))))


    (facts "About Swagger API"
      (fact "is valid"
        (compojure.api.validator/validate app) =not=> nil))

    (facts "About invalid routes"
      (let [apikey TOKEN]
        (fact "invalid path =>  404"
          (let [response (app (mock-req (mock/request :get "/invalid/path") apikey))]
            response => nil?))))


    (facts "About annotations"
      (let [apikey TOKEN]
        (against-background [(teams/get-teams anything) => TEAMS
                             (auth/get-permissions anything) => PERMISSIONS
                             (auth/identity anything) => ..auth..
                             (request-context/make-context anything org anything anything) => ..ctx..
                             ..ctx.. =contains=> {::request-context/routes ..rt..}]
          (facts "GET /entities/:id/annotations/:type"
            (let [id   (str (util/make-uuid))
                  tags [{:_id             (str (util/make-uuid))
                         :entity          id
                         :user            (str (util/make-uuid))
                         :type            "Annotation"
                         :annotation_type "tags"
                         :annotation      {:tag "--tag--"}}]]
              (against-background [(annotations/get-annotations ..ctx.. db [id] "tags") => tags]
                (fact "returns annotations by entity and user"
                  (let [path (util/join-path ["" "api/v1" ORGS org "entities" id "annotations" "tags"])
                        {:keys [status body]} (get* app path apikey)]
                    status => 200
                    body => {:tags tags})))))

          (facts "POST /entities/:id/annotations/:type"
            (let [id   (str (util/make-uuid))
                  post {:tags [{:tag "--tag--"}]}
                  tags [{:_id             (str (util/make-uuid))
                         :entity          id
                         :user            (str (util/make-uuid))
                         :type            "Annotation"
                         :annotation_type "tags"
                         :annotation      {:tag "--tag--"}}]]
              (against-background [(annotations/create-annotations ..ctx.. db [id] "tags" (:tags post)) => tags]
                (fact "creates annotations"
                  (let [path (util/join-path ["" "api/v1" ORGS org "entities" id "annotations/tags"])
                        {:keys [status body]} (post* app path apikey post)]
                    status => 201
                    body => {:tags tags})))))

          (facts "PUT /entities/:id/annotations/:type"
            (let [entity-id (util/make-uuid)
                  note-id   (util/make-uuid)
                  user-id   (util/make-uuid)
                  update    {:_id             (str note-id)
                             :entity          (str entity-id)
                             :user            (str user-id)
                             :type            "Annotation"
                             :annotation_type c/NOTES
                             :annotation      {:text      "--note--"
                                               :timestamp (util/iso-short-now)}}]
              (against-background [(annotations/update-annotation ..ctx.. db (str note-id) (:annotation update)) => update]
                (fact "updates annotation"
                  (let [path (str "/api/v1/" ORGS "/" org "/entities/" entity-id "/annotations/notes/" note-id)
                        {:keys [status body]} (put* app path apikey {:note (:annotation update)})]
                    status => 200
                    body => {:note update}))))))

        (facts "DELETE /entities/:id/annotations/:type/:annotation-id"
          (let [id            (str (util/make-uuid))
                annotation-id (str (util/make-uuid))
                tags          [{:_id             annotation-id
                                :entity          id
                                :user            (str (util/make-uuid))
                                :type            "Annotation"
                                :annotation_type "tags"
                                :annotation      {:tag "--tag--"}}]]
            (against-background [(teams/get-teams anything) => TEAMS
                                 (auth/get-permissions anything) => PERMISSIONS
                                 (request-context/make-context anything org anything anything) => ..ctx..
                                 (annotations/delete-annotations ..ctx.. db [annotation-id]) => tags]
              (fact "deletes annotations"
                (let [path (str "/api/v1/" ORGS "/" org "/entities/" id "/annotations/tags/" annotation-id)
                      {:keys [status body]} (delete* app path apikey)]
                  status => 202
                  body => {:tags tags}))))))

      (facts "About /entities"
        (let [apikey TOKEN]
          (against-background [(teams/get-teams anything) => TEAMS
                               (auth/get-permissions anything) => PERMISSIONS
                               (request-context/make-context anything org anything anything) => ..ctx..]

            (facts "read"
              (let [id  (str (UUID/randomUUID))
                    get (mock-req (mock/request :get (util/join-path ["" "api" ver/version ORGS org "entities" id])) apikey)
                    doc {:_id           id
                         :type          "Entity"
                         :links         {:self "self"}
                         :relationships {}
                         :attributes    {}}]

                (against-background [(core/get-entities ..ctx.. db [id] :include-trashed false) => [doc]
                                     (request-context/router anything) => ..rt..]
                  (fact "GET /entities/:id returns status 200"
                    (:status (app get)) => 200)
                  (fact "GET /entities/:id returns doc"
                    (body-json app get) => {:entity doc})))
              (let [id   (str (UUID/randomUUID))
                    get  (mock-req (mock/request :get (str (util/join-path ["" "api" ver/version ORGS org "entities" id]) "?includes=annotations")) apikey)
                    doc  {:_id           id
                          :type          "Entity"
                          :links         {:self "self"}
                          :relationships {}
                          :attributes    {}}

                    tag  {:_id             (str (UUID/randomUUID))
                          :entity          id
                          :user            (str (UUID/randomUUID))
                          :type            "Annotation"
                          :annotation_type c/TAGS
                          :annotation      {}}

                    prop {:_id             (str (UUID/randomUUID))
                          :entity          id
                          :user            (str (UUID/randomUUID))
                          :type            "Annotation"
                          :annotation_type c/PROPERTIES
                          :annotation      {}}

                    note {:_id             (str (UUID/randomUUID))
                          :entity          id
                          :user            (str (UUID/randomUUID))
                          :type            "Annotation"
                          :annotation_type c/NOTES
                          :annotation      {}}]

                (against-background [(core/get-entities ..ctx.. db [id] :include-trashed false) => [doc]
                                     (request-context/router anything) => ..rt..
                                     (annotations/get-annotations ..ctx.. db [id] c/TAGS) => [tag]
                                     (annotations/get-annotations ..ctx.. db [id] c/PROPERTIES) => [prop]
                                     (annotations/get-annotations ..ctx.. db [id] c/NOTES) => [note]
                                     (annotations/get-annotations ..ctx.. db [id] c/TIMELINE_EVENTS) => []]
                  (fact "GET /entities/:id returns status 200 "
                    (:status (app get)) => 200)
                  (fact "GET /entities/:id includes annotations"
                    (body-json app get) => {:entity   doc
                                            :includes [tag prop note]}))))))))

    (facts "About entities"
      (entity-resource-deletion-tests app db org "entitie"))

    (facts "About Projects"
      (entity-resource-create-tests app db org "Project")
      (entity-resources-create-tests app db org "Project")

      (entity-resources-read-tests app db org "Project")
      (entity-resource-read-tests app db org "Project")
      (entity-resource-update-tests app db org "Project")
      (entity-resource-deletion-tests app db org "Project"))

    (facts "About Sources"
      (entity-resources-read-tests app db org "Source")
      (entity-resource-read-tests app db org "Source")
      (entity-resource-create-tests app db org "Source")
      (entity-resource-update-tests app db org "Source")
      (entity-resource-deletion-tests app db org "Source"))

    (facts "About Folders"
      (entity-resources-read-tests app db org "Folder")
      (entity-resource-read-tests app db org "Folder")
      (entity-resource-create-tests app db org "Folder")
      (entity-resource-update-tests app db org "Folder")
      (entity-resource-deletion-tests app db org "Folder"))

    (facts "About Files"
      (entity-resource-read-tests app db org "File")
      (entity-resources-read-tests app db org "File")
      (entity-resource-update-tests app db org "File")
      (entity-resource-deletion-tests app db org "File")

      (facts "related Sources"
        (let [apikey TOKEN]
          (against-background [(teams/get-teams anything) => TEAMS
                               (auth/get-permissions anything) => PERMISSIONS
                               (request-context/make-context anything org anything anything) => ..ctx..]
            (future-fact "associates created Source")))))

    (facts "About Activities"
      (entity-resources-read-tests app db org "Activity")

      (entity-resource-read-tests app db org "Activity")
      (entity-resource-update-tests app db org "Activity")
      (entity-resource-deletion-tests app db org "Activity"))

    (facts "About revisions routes"
      (facts "/files/:id/HEAD"
        (fact "returns HEAD revisions"
          (let [apikey TOKEN
                id     (str (UUID/randomUUID))
                doc    {:_id           id
                        :type          c/FILE-TYPE
                        :links         {:self "self"}
                        :relationships {}
                        :attributes    {}}
                revs   [{:_id           id
                         :type          c/REVISION-TYPE
                         :links         {:self "self"}
                         :relationships {}
                         :attributes    {:content_type ""
                                         :name         ""
                                         :url          ""
                                         :previous     [(str (util/make-uuid))]
                                         :file_id      (str (util/make-uuid))}}]
                get    (mock-req (mock/request :get (util/join-path ["" "api" ver/version ORGS org "files" id "heads"])) apikey)]
            (body-json app get) => {:revisions revs}
            (provided
              (teams/get-teams anything) => TEAMS
              (auth/get-permissions anything) => PERMISSIONS
              (request-context/make-context anything org anything anything) => ..ctx..
              ..ctx.. =contains=> {::request-context/routes ..rt..}
              (revisions/get-head-revisions ..ctx.. db id) => revs)))))

    (facts "/move"
      (fact "moves file"
        (let [apikey   TOKEN
              id       (str (util/make-uuid))
              body     {:source      (str (util/make-uuid))
                        :destination (str (util/make-uuid))}
              post     (mock-req (-> (mock/request :post (util/join-path ["" "api" ver/version ORGS org "files" id "move"]))
                                   (mock/body (json-post-body body))) apikey)
              expected {:something "awesome"}]
          (body-json app post) => expected
          (provided
            (rh/move-contents* anything db org anything anything id body) => expected
            (request-context/make-context anything org anything anything) => ..ctx..
            (routes/self-route ..ctx.. "file" id) => "location")))

      (fact "moves folder"
        (let [apikey   TOKEN
              id       (str (util/make-uuid))
              body     {:source      (str (util/make-uuid))
                        :destination (str (util/make-uuid))}
              post     (mock-req (-> (mock/request :post (util/join-path ["" "api" ver/version ORGS org "folders" id "move"]))
                                   (mock/body (json-post-body body))) apikey)
              expected {:something "awesome"}]
          (body-json app post) => expected
          (provided
            (rh/move-contents* anything db org anything anything id body) => expected
            (request-context/make-context anything org anything anything) => ..ctx..
            (routes/self-route ..ctx.. "folder" id) => "location"))))

    (facts "About Teams API"
      (facts "GET /teams/:id"
        (fact "returns team"
          (let [apikey TOKEN
                id     (str (util/make-uuid))
                get    (mock-req (mock/request :get (util/join-path ["" "api" ver/version ORGS org "teams" id])) apikey)
                team   {:id                  "1"
                        :type                "Team"
                        :name                id
                        :uuid                id
                        :roles               []

                        :memberships         [{:id                  1774,
                                               :team_id             573,
                                               :added               "2016-02-01T21:00:00.000Z",
                                               :email               "existingmember@example.com",
                                               :role                {
                                                                     :id              184,
                                                                     :organization_id 63,
                                                                     :name            "Member",
                                                                     :links           {:permissions "/api/v1/permissions?role_id=184"}},
                                               :type                "Membership",
                                               :user_id             8

                                               :links               {:self "--self--"}}]
                        :links               {:self        "--url--"
                                              :memberships "--membership--url--"}
                        :team_groups         []}]
            (body-json app get) => {:team team}
            (provided
              (teams/get-teams anything) => TEAMS
              (auth/get-permissions anything) => PERMISSIONS
              (teams/get-team* anything anything id) => {:team team})))))

    (facts "About activity user stories"
      (facts "create project activity")
      (facts "create folder activity"))

    (facts "About provenance"
      (fact "/prov/:id returns local provenance"
        (let [apikey   TOKEN
              id       (str (UUID/randomUUID))
              expected [{:_id     id
                         :type    "Activity"
                         :name    "Something"
                         :inputs  []
                         :outputs []
                         :operators []}]
              get      (mock-req (mock/request :get (util/join-path ["" "api" ver/version ORGS org "prov" id])) apikey)]
          (body-json app get) => {:provenance expected}
          (provided
            (teams/get-teams anything) => TEAMS
            (auth/get-permissions anything) => PERMISSIONS
            (request-context/make-context anything org anything anything) => ..ctx..
            ..ctx.. =contains=> {::request-context/routes ..rt..}
            (prov/local ..ctx.. db [id]) => expected
            (serialize/entities expected) => expected))))))

    ;(facts "About breadcrumbs"
    ;  (facts "POST"
    ;    (fact "returns file breadcrumbs"
    ;      (let [id1      (str (UUID/randomUUID))
    ;            id2      (str (UUID/randomUUID))
    ;            folder1  (str (UUID/randomUUID))
    ;            folder2  (str (UUID/randomUUID))
    ;            project1 (str (UUID/randomUUID))
    ;            project2 (str (UUID/randomUUID))
    ;            apikey   TOKEN
    ;            get      (mock-req (-> (mock/request :post (util/join-path ["" "api" ver/version ORGS org-id "breadcrumbs"]))
    ;                                 (mock/body (json-post-body [id1 id2]))) apikey)
    ;            expected {(keyword id1) [[{:type c/FILE-TYPE :id id1 :name "filename1"}
    ;                                      {:type c/FOLDER-TYPE :id folder1 :name "foldername1"}
    ;                                      {:type c/PROJECT-TYPE :id project1 :name "projectname1"}]
    ;                                     [{:type c/FILE-TYPE :id id1 :name "filename1"}
    ;                                      {:type c/FOLDER-TYPE :id folder2 :name "foldername2"}
    ;                                      {:type c/PROJECT-TYPE :id project1 :name "projectname1"}]
    ;                                     [{:type c/FILE-TYPE :id id1 :name "filename1"}
    ;                                      {:type c/FOLDER-TYPE :id folder2 :name "foldername2"}
    ;                                      {:type c/PROJECT-TYPE :id project2 :name "projectname2"}]]
    ;                      (keyword id2) [[{:type c/FILE-TYPE :id id2 :name "filename2"}
    ;                                      {:type c/FOLDER-TYPE :id folder2 :name "foldername2"}
    ;                                      {:type c/PROJECT-TYPE :id project1 :name "projectname1"}]
    ;                                     [{:type c/FILE-TYPE :id id2 :name "filename2"}
    ;                                      {:type c/FOLDER-TYPE :id folder2 :name "foldername2"}
    ;                                      {:type c/PROJECT-TYPE :id project2 :name "projectname2"}]]}]
    ;        (body-json app get) => {:breadcrumbs expected}
    ;        (provided
    ;          (auth/identity anything) => ..auth..
    ;          (ovation.routes/router anything) => ..rt..
    ;          (b/get-parents ..auth.. db org-id id1 ..rt..) => [{:_id folder1} {:_id folder2}]
    ;          (b/get-parents ..auth.. db org-id id2 ..rt..) => [{:_id folder2}]
    ;          (b/get-parents ..auth.. db org-id folder1 ..rt..) => [{:_id project1}]
    ;          (b/get-parents ..auth.. db org-id folder2 ..rt..) => [{:_id project1} {:_id project2}]
    ;          (b/get-parents ..auth.. db org-id project1 ..rt..) => []
    ;          (b/get-parents ..auth.. db org-id project2 ..rt..) => []
    ;          (core/get-entities ..auth.. db org-id #{id1 folder1 folder2 id2 project1 project2} ..rt..) => [{:_id        id1
    ;                                                                                                          :type       c/FILE-TYPE
    ;                                                                                                          :attributes {:name "filename1"}}
    ;                                                                                                         {:_id        id2
    ;                                                                                                          :type       c/FILE-TYPE
    ;                                                                                                          :attributes {:name "filename2"}}
    ;                                                                                                         {:_id        folder1
    ;                                                                                                          :attributes {:name "foldername1"}}
    ;                                                                                                         {:_id        folder2
    ;                                                                                                          :type       c/FOLDER-TYPE
    ;                                                                                                          :attributes {:name "foldername2"}}
    ;                                                                                                         {:_id        project1
    ;                                                                                                          :type       c/PROJECT-TYPE
    ;                                                                                                          :attributes {:name "projectname1"}}
    ;                                                                                                         {:_id        project2
    ;                                                                                                          :type       c/PROJECT-TYPE
    ;                                                                                                          :attributes {:name "projectname2"}}]))))
    ;  (facts "GET"
    ;    (fact "returns file breadcrumbs"
    ;      (let [id1      (str (UUID/randomUUID))
    ;            id2      (str (UUID/randomUUID))
    ;            folder1  (str (UUID/randomUUID))
    ;            folder2  (str (UUID/randomUUID))
    ;            project1 (str (UUID/randomUUID))
    ;            project2 (str (UUID/randomUUID))
    ;            apikey   TOKEN
    ;            get      (mock-req (mock/request :get (str (util/join-path ["" "api" ver/version ORGS org-id "breadcrumbs"]) "?id=" id1)) apikey)
    ;            expected [[{:type c/FILE-TYPE :id id1 :name "filename1"}
    ;                       {:type c/FOLDER-TYPE :id folder1 :name "foldername1"}
    ;                       {:type c/PROJECT-TYPE :id project1 :name "projectname1"}]
    ;                      [{:type c/FILE-TYPE :id id1 :name "filename1"}
    ;                       {:type c/FOLDER-TYPE :id folder2 :name "foldername2"}
    ;                       {:type c/PROJECT-TYPE :id project1 :name "projectname1"}]
    ;                      [{:type c/FILE-TYPE :id id1 :name "filename1"}
    ;                       {:type c/FOLDER-TYPE :id folder2 :name "foldername2"}
    ;                       {:type c/PROJECT-TYPE :id project2 :name "projectname2"}]]]
    ;        (body-json app get) => {:breadcrumbs expected}
    ;        (provided
    ;          (auth/identity anything) => ..auth..
    ;          (ovation.routes/router anything) => ..rt..
    ;          (b/get-parents ..auth.. db org-id id1 ..rt..) => [{:_id folder1} {:_id folder2}]
    ;          (b/get-parents ..auth.. db org-id folder1 ..rt..) => [{:_id project1}]
    ;          (b/get-parents ..auth.. db org-id folder2 ..rt..) => [{:_id project1} {:_id project2}]
    ;          (b/get-parents ..auth.. db org-id project1 ..rt..) => []
    ;          (b/get-parents ..auth.. db org-id project2 ..rt..) => []
    ;          (core/get-entities ..auth.. db org-id #{id1 folder1 folder2 project1 project2} ..rt..) => [{:_id id1
    ;                                                                                               :type        c/FILE-TYPE
    ;                                                                                               :attributes  {:name "filename1"}}
    ;                                                                                              {:_id        folder1
    ;                                                                                               :type       c/FOLDER-TYPE
    ;                                                                                               :attributes {:name "foldername1"}}
    ;                                                                                              {:_id        folder2
    ;                                                                                               :type       c/FOLDER-TYPE
    ;                                                                                               :attributes {:name "foldername2"}}
    ;                                                                                              {:_id        project1
    ;                                                                                               :type       c/PROJECT-TYPE
    ;                                                                                               :attributes {:name "projectname1"}}
    ;                                                                                              {:_id        project2
    ;                                                                                               :type       c/PROJECT-TYPE
    ;                                                                                               :attributes {:name "projectname2"}}]))))

