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
            [ovation.constants :as k]
            [ovation.revisions :as revisions]
            [ovation.teams :as teams]
            [ovation.prov :as prov]
            [ovation.config :as config]
            [ovation.route-helpers :as rh]
            [buddy.sign.jwt :as jwt]
            [ovation.constants :as c]
            [ovation.breadcrumbs :as b]
            [ovation.routes :as routes]
            [compojure.api.validator]
            [ovation.test.system :as test.system]
            [ovation.system :as system])
  (:import (java.util UUID)))

(def id {:uuid (UUID/randomUUID)})

(def TOKEN (jwt/sign id (config/config :jwt-secret)))

(def TEAMS (promise))
(deliver TEAMS [])

(def PERMISSIONS (promise))
(deliver PERMISSIONS {})


(defn sling-throwable
  [exception-map]
  (slingshot.support/get-throwable (slingshot.support/make-context
                                     exception-map
                                     (str "throw+: " map)
                                     nil
                                     (slingshot.support/stack-trace))))


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
  [app db entity-type]
  (let [type-name (capitalize entity-type)
        type-path (typepath type-name)]
    `(let [apikey# TOKEN]
       (against-background [(teams/get-teams anything) => TEAMS
                            (auth/permissions anything) => PERMISSIONS
                            (auth/identity anything) => ..auth..]
         (facts ~(util/join-path ["" type-path])
           (facts "resources"
             (let [id#     (str (UUID/randomUUID))
                   entity# {:_id           id#
                            :_rev          "123"
                            :type          ~type-name
                            :attributes    {}
                            :links         {:self "self"}
                            :relationships {}}]
               (let [get-req# (mock-req (mock/request :get (util/join-path ["" "api" ~ver/version ~type-path])) apikey#)]
                 (against-background [(core/of-type ..auth.. ~db ~type-name ..rt..) => [entity#]
                                      (r/router anything) => ..rt..]
                   (fact ~(str "GET / gets all " type-path)
                     (body-json get-req#) => {~(keyword type-path) [entity#]}))))))))))


(defmacro entity-resource-read-tests
  "Facts about reading resource"
  [app db entity-type]
  (let [type-name (capitalize entity-type)
        type-path (typepath type-name)]
    `(let [apikey# TOKEN]

       (against-background [(teams/get-teams anything) => TEAMS
                            (auth/permissions anything) => PERMISSIONS
                            (auth/identity anything) => ..auth..]
         (facts ~(util/join-path ["" type-path])
           (facts "resource"
             (let [id#     (str (UUID/randomUUID))
                   entity# {:_id           id#
                            :_rev          "123"
                            :type          ~type-name
                            :attributes    {}
                            :links         {:self "self"}
                            :relationships {}}]
               (let [get-req# (mock-req (mock/request :get (util/join-path ["" "api" ~ver/version ~type-path id#])) apikey#)]
                 (against-background [(core/get-entities ..auth.. ~db [id#] ..rt..) => [entity#]
                                      (r/router anything) => ..rt..]
                   (fact ~(str "GET /:id gets a single " (lower-case type-name))
                     (body-json get-req#) => {~(keyword (lower-case type-name)) entity#})
                   (let [source# {:_id        id#
                                  :_rev       "123"
                                  :type       "OtherType"
                                  :attributes {}
                                  :links      {:self ""}}]
                     (fact ~(str "GET /:id returns 404 if not a " (lower-case type-name))
                       (:status (~app get-req#)) => 404
                       (provided
                         (core/get-entities ..auth.. ~db [id#] ..rt..) => [source#]))))))))))))


(defmacro entity-resource-create-tests
  "Facts about a resource creation (e.g. \"Project\")"
  [app db entity-type]

  (let [type-name (capitalize entity-type)
        type-path (typepath type-name)]
    `(let [apikey# TOKEN]
       (against-background [(teams/get-teams anything) => TEAMS
                            (auth/permissions anything) => PERMISSIONS
                            (teams/create-team anything anything) => {:team ..team..}
                            (auth/identity anything) => ..auth..]
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

                   request#      (fn [] (mock-req (-> (mock/request :post (util/join-path ["" "api" ~ver/version ~type-path (:_id parent#)]))
                                                    (mock/body (json/write-str (walk/stringify-keys new-entities#)))) apikey#))
                   links#        [{:type "Relation" :foo "bar"}]]


               (against-background [(core/create-entities ..auth.. ~db [new-entity#] ..rt.. :parent (:_id parent#)) => [entity#]
                                    (core/get-entities ..auth.. ~db [(:_id parent#)] ..rt..) => [parent#]
                                    (links/add-links ..auth.. ~db [parent#] rel# [(:_id entity#)] ..rt.. :inverse-rel inverse_rel#) => {:links links#}
                                    (core/create-values ..auth.. ~db ..rt.. links#) => links#
                                    (core/update-entities ..auth.. ~db anything ..rt.. :authorize false :update-collaboration-roots true) => ..updates..
                                    (r/router anything) => ..rt..]
                 (fact "POST /:id returns status 201"
                   (let [post# (request#)]
                     (:status (~app post#)) => 201))
                 (fact ~(str "POST /:id inserts entity with " type-name " parent")
                   (let [post# (request#)]
                     (body-json post#) => {:entities [entity#]
                                           :links    links#
                                           :updates  {}})))

               (fact "POST /:id returns 401 if not can? :create"
                 (:status (~app (request#))) => 401
                 (provided
                   (r/router anything) => ..rt..
                   (core/create-entities ..auth.. ~db [new-entity#] ..rt.. :parent (:_id parent#)) =throws=> (sling-throwable {:type :ovation.auth/unauthorized}))))))))))


(defmacro entity-resources-create-tests
  "Facts about a resource creation (e.g. \"Project\")"
  [app db entity-type]

  (let [type-name (capitalize entity-type)
        type-path (typepath type-name)]
    `(let [apikey# TOKEN]

       (against-background [(teams/get-teams anything) => TEAMS
                            (auth/permissions anything) => PERMISSIONS
                            (teams/create-team anything anything) => {:team ..team..}
                            (auth/identity anything) => ..auth..]
         (facts ~(util/join-path ["" type-path])
           (facts "create"
             (let [new-entity#         {:type ~(capitalize type-name) :attributes {:foo "bar"}}
                   plural-source-type# ~(util/entity-type-name-keyword type-path)

                   new-entities#       {plural-source-type# [new-entity#]}
                   entity#             (assoc new-entity# :_id ~(str (UUID/randomUUID))
                                                          :_rev "1")
                   request#            (fn [] (mock-req (-> (mock/request :post (util/join-path ["" "api" ~ver/version ~type-path]))
                                                          (mock/body (json/write-str (walk/stringify-keys new-entities#)))) apikey#))]

               (against-background [(core/create-entities ..auth.. ~db [new-entity#] ..rt..) => [entity#]
                                    (core/create-values ..auth.. ~db ..rt.. []) => []
                                    (core/update-entities ..auth.. ~db [] ..rt.. :authorize false :update-collaboration-roots true) => []
                                    (r/router anything) => ..rt..]
                 (fact "POST / returns status 201"
                   (let [post# (request#)]
                     (:status (~app post#)) => 201))
                 (fact ~(str "POST / inserts new top-level " type-name)
                   (let [post# (request#)]
                     (body-json post#) => {~(keyword type-path) [entity#]})))


               (let [bad-entities# [{:type "Other" :attributes {:foo "bar"}}]
                     bad-request#  (fn [] (mock-req (-> (mock/request :post (util/join-path ["" "api" ~ver/version ~type-path]))
                                                      (mock/body (json/write-str (walk/stringify-keys bad-entities#)))) apikey#))]
                 (fact "POST / returns 400 if type does not match"
                   (:status (~app (bad-request#))) => 400))

               (fact "POST / returns 401 if not can? :create"
                 (:status (~app (request#))) => 401
                 (provided
                   (r/router anything) => ..rt..
                   (core/create-entities ..auth.. ~db [new-entity#] ..rt..) =throws=> (sling-throwable {:type :ovation.auth/unauthorized}))))))))))


(defmacro entity-resource-update-tests
  "Facts about a resource update (e.g. \"Project\")"
  [app db entity-type]
  (let [type-name (capitalize entity-type)
        type-path (typepath type-name)]
    `(let [apikey# TOKEN]

       (against-background [(teams/get-teams anything) => TEAMS
                            (auth/permissions anything) => PERMISSIONS
                            (auth/identity anything) => ..auth..]
         (facts ~(util/join-path ["" type-path])
           (facts "update"
             (let [id#             (UUID/randomUUID)
                   attributes#     {:foo "bar"}
                   entity#         {:type          ~type-name
                                    :_id           id#
                                    :_rev          "1"
                                    :attributes    attributes#
                                    :links         {:self "self"}
                                    :relationships {}}
                   new-attributes# {:bar "baz"}
                   update#         (-> entity#
                                     (dissoc :links)
                                     (dissoc :relationships)
                                     (assoc :attributes new-attributes#))
                   put-body#       {~(util/entity-type-name-keyword type-name) (assoc update# :_id (str id#))}
                   updated-entity# (assoc update# :_rev "2" :links {:self "self"} :relationships {} :_id (str id#))
                   request#        (fn [entity-id#] (mock-req (-> (mock/request :put (util/join-path ["" "api" ~ver/version ~type-path (str entity-id#)]))
                                                                (mock/body (json/write-str (walk/stringify-keys put-body#)))) apikey#))]

               (against-background [(core/update-entities ..auth.. ~db [update#] ..rt..) => [updated-entity#]
                                    (r/router anything) => ..rt..]
                 (fact "succeeds with status 200"
                   (let [response# (~app (request# id#))]
                     (:status response#) => 200))
                 (fact "updates single entity by ID"
                   (let [request# (request# id#)]
                     (body-json request#)) => {~(util/entity-type-name-keyword type-name) updated-entity#})
                 (fact "fails if entity and path :id do not match"
                   (let [other-id# (str (UUID/randomUUID))
                         response# (~app (request# other-id#))]
                     (:status response#) => 404)))

               ;(against-background [(r/router anything) => ..rt..]
               ;  (fact "fails with status 409"
               ;    (let [response# (app (request# id#))]
               ;      (:status response#) => 409
               ;      (provided
               ;        (core/update-entities ..auth.. [update#] ..rt..) =throws=> (sling-throwable {:status 409})))))

               (fact "fails if not can? :update"
                 (:status (~app (request# id#))) => 401
                 (provided
                   (r/router anything) => ..rt..
                   (core/update-entities ..auth.. ~db [update#] ..rt..) =throws=> (sling-throwable {:type :ovation.auth/unauthorized}))))))))))


(defmacro entity-resource-deletion-tests
  "Facts about a resource type (e.g. \"Project\")"
  [app db entity-type]
  (let [type-name (capitalize entity-type)
        type-path (typepath type-name)]
    `(let [apikey# TOKEN]

       (against-background [(teams/get-teams anything) => TEAMS
                            (auth/permissions anything) => PERMISSIONS
                            (auth/identity anything) => ..auth..]

         (facts ~(util/join-path ["" type-path])
           (facts "delete"
             (let [id#             (UUID/randomUUID)
                   attributes#     {:foo "bar"}
                   entity#         {:type       "MyEntity"
                                    :_id        (str id#)
                                    :_rev       "1"
                                    :attributes attributes#
                                    :links      {:self "self"}}
                   deleted-entity# (assoc entity# :transh_info {:trashing_user (str (UUID/randomUUID))
                                                                :trasing_date  "1971-12-01"
                                                                :trash_root    ""})
                   request#        (fn [entity-id#] (mock-req (-> (mock/request :delete (util/join-path ["" "api" ~ver/version ~type-path (str entity-id#)]))) apikey#))]

               (against-background [(core/delete-entities ..auth.. ~db [(str id#)] ..rt..) => [deleted-entity#]
                                    (r/router anything) => ..rt..]
                 (fact "succeeds with status 202"
                   (let [response# (~app (request# id#))]
                     (:status response#) => 202))
                 (fact "DELETE /:id trashes entity"
                   (let [delete-request# (request# id#)]
                     (body-json delete-request#)) => {:entity deleted-entity#}))

               (fact "returns 401 if not can? :delete"
                 (:status (~app (request# id#))) => 401
                 (provided
                   (r/router anything) => ..rt..
                   (core/delete-entities ..auth.. ~db [(str id#)] ..rt..) =throws=> (sling-throwable {:type :ovation.auth/unauthorized}))))))))))


(against-background [(around :contents (test.system/system-background ?form))]
  (let [app (test.system/get-app)
        db  (test.system/get-db)]

    (defn body-json
      [request]
      (let [response (app request)
            reader   (clojure.java.io/reader (:body response))
            result   (json/read reader)]
        (walk/keywordize-keys result)))

    (facts "About authorization"
      (fact "invalid API key returns 401"
        (let [apikey "12345"
              get    (mock-req (mock/request :get "/api/v1/entities/123") apikey)]
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
                             (auth/permissions anything) => PERMISSIONS
                             (auth/identity anything) => ..auth..
                             (routes/router anything) => ..rt..]
          (facts "GET /entities/:id/annotations/:type"
            (let [id   (str (util/make-uuid))
                  tags [{:_id             (str (util/make-uuid))
                         :_rev            "1"
                         :entity          id
                         :user            (str (util/make-uuid))
                         :type            "Annotation"
                         :annotation_type "tags"
                         :annotation      {:tag "--tag--"}}]]
              (against-background [(annotations/get-annotations ..auth.. db [id] "tags" ..rt..) => tags]
                (fact "returns annotations by entity and user"
                  (let [path (str "/api/v1/entities/" id "/annotations/tags")
                        {:keys [status body]} (get* app path apikey)]
                    status => 200
                    body => {:tags tags})))))

          (facts "POST /entities/:id/annotations/:type"
            (let [id   (str (util/make-uuid))
                  post {:tags [{:tag "--tag--"}]}
                  tags [{:_id             (str (util/make-uuid))
                         :_rev            "1"
                         :entity          id
                         :user            (str (util/make-uuid))
                         :type            "Annotation"
                         :annotation_type "tags"
                         :annotation      {:tag "--tag--"}}]]
              (against-background [(annotations/create-annotations ..auth.. db anything [id] "tags" (:tags post)) => tags]
                (fact "creates annotations"
                  (let [path (str "/api/v1/entities/" id "/annotations/tags")
                        {:keys [status body]} (post* app path apikey post)]
                    status => 201
                    body => {:tags tags})))))

          (facts "PUT /entities/:id/annotations/:type"
            (let [entity-id (util/make-uuid)
                  note-id   (util/make-uuid)
                  user-id   (util/make-uuid)
                  update    {:_id             (str note-id)
                             :_rev            "1"
                             :entity          (str entity-id)
                             :user            (str user-id)
                             :type            "Annotation"
                             :annotation_type c/NOTES
                             :annotation      {:text      "--note--"
                                               :timestamp (util/iso-short-now)}}]
              (against-background [(annotations/update-annotation ..auth.. db anything (str note-id) (:annotation update)) => update]
                (fact "updates annotation"
                  (let [path (str "/api/v1/entities/" entity-id "/annotations/notes/" note-id)
                        {:keys [status body]} (put* app path apikey {:note (:annotation update)})]
                    status => 200
                    body => {:note update}))))))

        (facts "DELETE /entities/:id/annotations/:type/:annotation-id"
          (let [id            (str (util/make-uuid))
                annotation-id (str (util/make-uuid))
                tags          [{:_id             annotation-id
                                :_rev            "1"
                                :entity          id
                                :user            (str (util/make-uuid))
                                :type            "Annotation"
                                :annotation_type "tags"
                                :annotation      {:tag "--tag--"}}]]
            (against-background [(teams/get-teams anything) => TEAMS
                                 (auth/permissions anything) => PERMISSIONS
                                 (auth/identity anything) => ..auth..
                                 (annotations/delete-annotations ..auth.. db [annotation-id] anything) => tags]
              (fact "deletes annotations"
                (let [path (str "/api/v1/entities/" id "/annotations/tags/" annotation-id)
                      {:keys [status body]} (delete* app path apikey)]
                  status => 202
                  body => {:tags tags}))))))

      (facts "About /entities"
        (let [apikey TOKEN]
          (against-background [(teams/get-teams anything) => TEAMS
                               (auth/permissions anything) => PERMISSIONS
                               (auth/identity anything) => ..auth..]

            (facts "read"
              (let [id  (str (UUID/randomUUID))
                    get (mock-req (mock/request :get (util/join-path ["" "api" ver/version "entities" id])) apikey)
                    doc {:_id           id
                         :_rev          "123"
                         :type          "Entity"
                         :links         {:self "self"}
                         :relationships {}
                         :attributes    {}}]

                (against-background [(core/get-entities ..auth.. [id] ..rt.. :include-trashed false) => [doc]
                                     (r/router anything) => ..rt..]
                  (fact "GET /entities/:id returns status 200"
                    (:status (app get)) => 200)
                  (fact "GET /entities/:id returns doc"
                    (body-json get) => {:entity doc}))))))))

    (facts "About entities"
      (entity-resource-deletion-tests app db "entitie"))

    (facts "About Projects"
      (entity-resource-create-tests app db "Project")
      (entity-resources-create-tests app db "Project")

      (entity-resources-read-tests app db "Project")
      (entity-resource-read-tests app db "Project")
      (entity-resource-update-tests app db "Project")
      (entity-resource-deletion-tests app db "Project"))

    (facts "About Sources"
      (entity-resources-read-tests app db "Source")
      (entity-resource-read-tests app db "Source")
      (entity-resource-create-tests app db "Source")
      (entity-resources-create-tests app db "Source")
      (entity-resource-update-tests app db "Source")
      (entity-resource-deletion-tests app db "Source"))

    (facts "About Folders"
      (entity-resources-read-tests app db "Folder")
      (entity-resource-read-tests app db "Folder")
      (entity-resource-create-tests app db "Folder")
      (entity-resource-update-tests app db "Folder")
      (entity-resource-deletion-tests app db "Folder"))

    (facts "About Files"
      (entity-resource-read-tests app db "File")
      (entity-resources-read-tests app db "File")
      (entity-resource-update-tests app db "File")
      (entity-resource-deletion-tests app db "File")

      (facts "related Sources"
        (let [apikey TOKEN]
          (against-background [(teams/get-teams anything) => TEAMS
                               (auth/permissions anything) => PERMISSIONS
                               (auth/identity anything) => ..auth..]
            (future-fact "associates created Source")))))

    (facts "About Activities"
      (entity-resources-read-tests app db "Activity")

      (entity-resource-read-tests app db "Activity")
      (entity-resource-update-tests app db "Activity")
      (entity-resource-deletion-tests app db "Activity"))

    (facts "About revisions routes"
      (facts "/files/:id/HEAD"
        (fact "returns HEAD revisions"
          (let [apikey TOKEN
                id     (str (UUID/randomUUID))
                doc    {:_id           id
                        :_rev          "123"
                        :type          k/FILE-TYPE
                        :links         {:self "self"}
                        :relationships {}
                        :attributes    {}}
                revs   [{:_id           id
                         :_rev          "123"
                         :type          k/REVISION-TYPE
                         :links         {:self "self"}
                         :relationships {}
                         :attributes    {:content_type ""
                                         :name         ""
                                         :url          ""
                                         :previous     [(str (util/make-uuid))]
                                         :file_id      (str (util/make-uuid))}}]
                get    (mock-req (mock/request :get (util/join-path ["" "api" ver/version "files" id "heads"])) apikey)]
            (body-json get) => {:revisions revs}
            (provided
              (teams/get-teams anything) => TEAMS
              (auth/permissions anything) => PERMISSIONS
              (auth/identity anything) => ..auth..
              (r/router anything) => ..rt..
              (revisions/get-head-revisions ..auth.. db ..rt.. id) => revs)))))

    (facts "/move"
      (fact "moves file"
        (let [apikey   TOKEN
              id       (str (util/make-uuid))
              body     {:source      (str (util/make-uuid))
                        :destination (str (util/make-uuid))}
              post     (mock-req (-> (mock/request :post (util/join-path ["" "api" ver/version "files" id "move"]))
                                   (mock/body (json-post-body body))) apikey)
              expected {:something "awesome"}]
          (body-json post) => expected
          (provided
            (rh/move-contents* anything db id body) => expected)))

      (fact "moves folder"
        (let [apikey   TOKEN
              id       (str (util/make-uuid))
              body     {:source      (str (util/make-uuid))
                        :destination (str (util/make-uuid))}
              post     (mock-req (-> (mock/request :post (util/join-path ["" "api" ver/version "folders" id "move"]))
                                   (mock/body (json-post-body body))) apikey)
              expected {:something "awesome"}]
          (body-json post) => expected
          (provided
            (rh/move-contents* anything db id body) => expected))))

    (facts "About Teams API"
      (facts "GET /teams/:id"
        (fact "returns team"
          (let [apikey TOKEN
                id     (str (util/make-uuid))
                get    (mock-req (mock/request :get (util/join-path ["" "api" ver/version "teams" id])) apikey)
                team   {:id                  "1"
                        :type                "Team"
                        :name                id
                        :uuid                id
                        :roles               []
                        :pending_memberships [{
                                               :id    "232",
                                               :role  {
                                                       :id              184,
                                                       :organization_id 63,
                                                       :name            "Member",
                                                       :links           {:permissions "/api/v1/permissions?role_id=184"}},
                                               :added "2016-02-01T21:00:00.000Z",
                                               :email "newmember@example.com",
                                               :type  "PendingMembership"},
                                              {
                                               :id    "2323",
                                               :role  {
                                                       :id              184,
                                                       :organization_id 63,
                                                       :name            "Member",
                                                       :links           {:permissions "/api/v1/permissions?role_id=184"}},
                                               :added "2016-02-01T21:00:00.000Z",
                                               :email "newmember@example.com",
                                               :type  "PendingMembership"}]

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
                                               :user_id             8,
                                               :membership_role_ids [
                                                                     1526]

                                               :links               {:self "--self--"}}]
                        :links               {:self        "--url--"
                                              :memberships "--membership--url--"}}]
            (body-json get) => {:team             team
                                :users            []
                                :membership_roles []}
            (provided
              (teams/get-teams anything) => TEAMS
              (auth/permissions anything) => PERMISSIONS
              (teams/get-team* anything id) => {:team             team
                                                :users            []
                                                :membership_roles []})))))

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
                         :outputs []}]
              get      (mock-req (mock/request :get (util/join-path ["" "api" ver/version "prov" id])) apikey)]
          (body-json get) => {:provenance expected}
          (provided
            (teams/get-teams anything) => TEAMS
            (auth/permissions anything) => PERMISSIONS
            (auth/identity anything) => ..auth..
            (prov/local ..auth.. db ..rt.. [id]) => expected
            (r/router anything) => ..rt..))))

    (facts "About breadcrumbs"
      (facts "POST"
        (fact "returns file breadcrumbs"
          (let [id1      (str (UUID/randomUUID))
                id2      (str (UUID/randomUUID))
                folder1  (str (UUID/randomUUID))
                folder2  (str (UUID/randomUUID))
                project1 (str (UUID/randomUUID))
                project2 (str (UUID/randomUUID))
                apikey   TOKEN
                get      (mock-req (-> (mock/request :post (util/join-path ["" "api" ver/version "breadcrumbs"]))
                                     (mock/body (json-post-body [id1 id2]))) apikey)
                expected {(keyword id1) [[{:type k/FILE-TYPE :id id1 :name "filename1"}
                                          {:type k/FOLDER-TYPE :id folder1 :name "foldername1"}
                                          {:type k/PROJECT-TYPE :id project1 :name "projectname1"}]
                                         [{:type k/FILE-TYPE :id id1 :name "filename1"}
                                          {:type k/FOLDER-TYPE :id folder2 :name "foldername2"}
                                          {:type k/PROJECT-TYPE :id project1 :name "projectname1"}]
                                         [{:type k/FILE-TYPE :id id1 :name "filename1"}
                                          {:type k/FOLDER-TYPE :id folder2 :name "foldername2"}
                                          {:type k/PROJECT-TYPE :id project2 :name "projectname2"}]]
                          (keyword id2) [[{:type k/FILE-TYPE :id id2 :name "filename2"}
                                          {:type k/FOLDER-TYPE :id folder2 :name "foldername2"}
                                          {:type k/PROJECT-TYPE :id project1 :name "projectname1"}]
                                         [{:type k/FILE-TYPE :id id2 :name "filename2"}
                                          {:type k/FOLDER-TYPE :id folder2 :name "foldername2"}
                                          {:type k/PROJECT-TYPE :id project2 :name "projectname2"}]]}]
            (body-json get) => {:breadcrumbs expected}
            (provided
              (auth/identity anything) => ..auth..
              (ovation.routes/router anything) => ..rt..
              (b/get-parents ..auth.. db id1 ..rt..) => [{:_id folder1} {:_id folder2}]
              (b/get-parents ..auth.. db id2 ..rt..) => [{:_id folder2}]
              (b/get-parents ..auth.. db folder1 ..rt..) => [{:_id project1}]
              (b/get-parents ..auth.. db folder2 ..rt..) => [{:_id project1} {:_id project2}]
              (b/get-parents ..auth.. db project1 ..rt..) => []
              (b/get-parents ..auth.. db project2 ..rt..) => []
              (core/get-entities ..auth.. db #{id1 folder1 folder2 id2 project1 project2} ..rt..) => [{:_id        id1
                                                                                                       :type       k/FILE-TYPE
                                                                                                       :attributes {:name "filename1"}}
                                                                                                      {:_id        id2
                                                                                                       :type       k/FILE-TYPE
                                                                                                       :attributes {:name "filename2"}}
                                                                                                      {:_id        folder1
                                                                                                       :type       k/FOLDER-TYPE
                                                                                                       :attributes {:name "foldername1"}}
                                                                                                      {:_id        folder2
                                                                                                       :type       k/FOLDER-TYPE
                                                                                                       :attributes {:name "foldername2"}}
                                                                                                      {:_id        project1
                                                                                                       :type       k/PROJECT-TYPE
                                                                                                       :attributes {:name "projectname1"}}
                                                                                                      {:_id        project2
                                                                                                       :type       k/PROJECT-TYPE
                                                                                                       :attributes {:name "projectname2"}}]))))
      (facts "GET"
        (fact "returns file breadcrumbs"
          (let [id1      (str (UUID/randomUUID))
                id2      (str (UUID/randomUUID))
                folder1  (str (UUID/randomUUID))
                folder2  (str (UUID/randomUUID))
                project1 (str (UUID/randomUUID))
                project2 (str (UUID/randomUUID))
                apikey   TOKEN
                get      (mock-req (mock/request :get (str (util/join-path ["" "api" ver/version "breadcrumbs"]) "?id=" id1)) apikey)
                expected [[{:type k/FILE-TYPE :id id1 :name "filename1"}
                           {:type k/FOLDER-TYPE :id folder1 :name "foldername1"}
                           {:type k/PROJECT-TYPE :id project1 :name "projectname1"}]
                          [{:type k/FILE-TYPE :id id1 :name "filename1"}
                           {:type k/FOLDER-TYPE :id folder2 :name "foldername2"}
                           {:type k/PROJECT-TYPE :id project1 :name "projectname1"}]
                          [{:type k/FILE-TYPE :id id1 :name "filename1"}
                           {:type k/FOLDER-TYPE :id folder2 :name "foldername2"}
                           {:type k/PROJECT-TYPE :id project2 :name "projectname2"}]]]
            (body-json get) => {:breadcrumbs expected}
            (provided
              (auth/identity anything) => ..auth..
              (ovation.routes/router anything) => ..rt..
              (b/get-parents ..auth.. db id1 ..rt..) => [{:_id folder1} {:_id folder2}]
              (b/get-parents ..auth.. db folder1 ..rt..) => [{:_id project1}]
              (b/get-parents ..auth.. db folder2 ..rt..) => [{:_id project1} {:_id project2}]
              (b/get-parents ..auth.. db project1 ..rt..) => []
              (b/get-parents ..auth.. db project2 ..rt..) => []
              (core/get-entities ..auth.. db #{id1 folder1 folder2 project1 project2} ..rt..) => [{:_id        id1
                                                                                                   :type       k/FILE-TYPE
                                                                                                   :attributes {:name "filename1"}}
                                                                                                  {:_id        folder1
                                                                                                   :type       k/FOLDER-TYPE
                                                                                                   :attributes {:name "foldername1"}}
                                                                                                  {:_id        folder2
                                                                                                   :type       k/FOLDER-TYPE
                                                                                                   :attributes {:name "foldername2"}}
                                                                                                  {:_id        project1
                                                                                                   :type       k/PROJECT-TYPE
                                                                                                   :attributes {:name "projectname1"}}
                                                                                                  {:_id        project2
                                                                                                   :type       k/PROJECT-TYPE
                                                                                                   :attributes {:name "projectname2"}}])))))))
