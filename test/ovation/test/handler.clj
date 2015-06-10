(ns ovation.test.handler
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [ovation.handler :refer [app]]
            [clojure.data.json :as json]
            [slingshot.slingshot :refer [try+]]
            [ovation.auth :as auth]
            [ovation.core :as core]
            [clojure.walk :as walk]
            [ovation.util :as util]
            [ovation.version :as ver]
            [schema.core :as s]
            [ovation.links :as links]
            [clojure.string :refer [lower-case capitalize]])
  (:import (java.util UUID)))

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
    (mock/header "Authorization" (str "Token token=" apikey))
    (mock/content-type "application/json")))

(defn body-json
  [request]
  (let [response (app request)
        reader (clojure.java.io/reader (:body response))
        result (json/read reader)]
    (walk/keywordize-keys result)))

(facts "About authorization"
  (fact "invalid API key returns 401"
    (let [apikey "12345"
          get (mock-req (mock/request :get "/api/v1/entities/123") apikey)
          auth-response (promise)
          _ (deliver auth-response {:status 401})]
      (:status (app get)) => 401
      (provided
        (auth/get-auth anything apikey) => auth-response))))


(facts "About doc route"
  (let [apikey "..apikey.."
        auth-response (promise)
        _ (deliver auth-response {:status 200 :body "{\"user\": \"..user..\"}"})]
    (against-background [(auth/get-auth anything apikey) => auth-response]
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
  )

(facts "About Swagger API"
  (fact "is valid"
    (compojure.api.swagger/validate app) =not=> nil))

(facts "About invalid routes"
  (let [apikey "..apikey.."
        auth-response (promise)
        _ (deliver auth-response {:status 200 :body "{\"user\": \"..user..\"}"})]
    (against-background [(auth/get-auth anything apikey) => auth-response]
      (fact "invalid path =>  404"
        (let [response (app (mock-req (mock/request :get "/invalid/path") apikey))]
          response => nil?)))))



(facts "About /entities"
  (let [apikey "--apikey--"
        auth-info {:user "..user.."}]
    (against-background [(auth/authorize anything apikey) => auth-info]

      (facts "read"
        (let [id (str (UUID/randomUUID))
              get (mock-req (mock/request :get (util/join-path ["" "api" ver/version "entities" id])) apikey)
              doc {:_id        id
                   :_rev       "123"
                   :type       "Entity"
                   :links      {}
                   :attributes {}}]

          (against-background [(core/get-entities auth-info [id]) => [doc]]
            (fact "GET /entities/:id returns status 200"
              (:status (app get)) => 200)
            (fact "GET /entities/:id returns doc"
              (body-json get) => {:entity doc}))))

      (facts "update"
        (let [id (UUID/randomUUID)
              attributes {:foo "bar"}
              entity {:type       "MyEntity"
                      :_id        id
                      :_rev       "1"
                      :attributes attributes}
              new-attributes {:bar "baz"}
              update (assoc entity :attributes new-attributes)
              updated-entity (assoc update :_rev "2" :links {} :_id (str id))
              request (fn [entity-id] (mock-req (-> (mock/request :put (util/join-path ["" "api" ver/version "entities" (str entity-id)]))
                                                  (mock/body (json/write-str (walk/stringify-keys (assoc update :_id (str id)))))) apikey))]

          (against-background [(core/update-entity auth-info [update]) => [updated-entity]]
            (fact "succeeds with status 200"
              (let [response (app (request id))]
                (:status response) => 200))
            (fact "updates single entity by ID"
              (let [request (request id)]
                (body-json request)) => {:entities [updated-entity]})
            (fact "fails if entity and path :id do not match"
              (let [other-id (str (UUID/randomUUID))
                    response (app (request other-id))]
                (:status response) => 404)))

          (fact "fails if not can? :update"
            (:status (app (request id))) => 401
            (provided
              (core/update-entity auth-info [update]) =throws=> (sling-throwable {:type :ovation.auth/unauthorized})))
          )
        )

      (facts "create"
        (let [parent-id (str (UUID/randomUUID))
              new-entity {:type "MyEntity" :attributes {:foo "bar"}}
              new-entities [new-entity]
              entity [(assoc new-entity :_id (str (UUID/randomUUID))
                                        :_rev "1")]
              request (fn [] (mock-req (-> (mock/request :post (util/join-path ["" "api" ver/version "entities" parent-id]))
                                         (mock/body (json/write-str (walk/stringify-keys new-entities)))) apikey))]

          (against-background [(core/create-entity auth-info new-entities :parent parent-id) => [entity]]
            (fact "POST /:id returns status 201"
              (let [post (request)]
                (:status (app post)) => 201))
            (fact "POST /:id inserts entity with parent"
              (let [post (request)]
                (body-json post) => {:entities [entity]})))

          (fact "returns 401 if not can? :create"
            (:status (app (request))) => 401
            (provided
              (core/create-entity auth-info new-entities :parent parent-id) =throws=> (sling-throwable {:type :ovation.auth/unauthorized})))
          ))

      (facts "delete"
        (let [id (UUID/randomUUID)
              attributes {:foo "bar"}
              entity {:type       "MyEntity"
                      :_id        id
                      :_rev       "1"
                      :attributes attributes}
              deleted-entity (assoc entity :transh_info {} :_id (str id))
              request (fn [entity-id] (mock-req (-> (mock/request :delete (util/join-path ["" "api" ver/version "entities" (str entity-id)]))) apikey))]

          (against-background [(core/delete-entity auth-info [(str id)]) => [deleted-entity]]
            (fact "succeeds with status 202"
              (let [response (app (request id))]
                (:status response) => 202))
            (fact "DELETE /:id trashes entity"
              (let [request (request id)]
                (body-json request)) => {:entities [deleted-entity]}))

          (fact "fails if not can? :delete"
            (:status (app (request id))) => 401
            (provided
              (core/delete-entity auth-info [(str id)]) =throws=> (sling-throwable {:type :ovation.auth/unauthorized})))
          )))))


(facts "About links"
  (let [apikey "--apikey--"
        auth-user-id (str (UUID/randomUUID))
        auth-info {:user "..user.."
                   :uuid auth-user-id}
        id (str (UUID/randomUUID))
        rel "--myrel--"
        attributes {:foo "bar"}
        entity {:type       "MyEntity"
                :_id        id
                :_rev       "1"
                :attributes attributes
                :links      {}}]
    (against-background [(auth/authorize anything apikey) => auth-info]
      (facts "GET /entities/:id/links/:rel"
        (let [request (fn [id] (mock-req (mock/request :get (util/join-path ["" "api" ver/version "entities" id "links" rel])) apikey))]
          (against-background [(links/get-link-targets auth-info id rel) => [entity]]
            (fact "succeeds with 200"
              (let [response (app (request id))]
                (:status response) => 200))
            (fact "returns link targets"
              (body-json (request id)) => {:rel [entity]}))))
      (facts "POST /entities/:id/links/:rel"
        (let [targetid1 (str (UUID/randomUUID))
              targetid2 (str (UUID/randomUUID))
              target1 (assoc entity :_id targetid1)
              target2 (assoc entity :_id targetid2)
              links [{:target_id targetid1} {:target_id targetid2}]
              request (fn [id] (mock-req (-> (mock/request :post (util/join-path ["" "api" ver/version "entities" id "links" rel]))
                                           (mock/body (json/write-str (walk/stringify-keys links)))) apikey))]

          (against-background [(core/get-entities auth-info [id]) => [entity]
                               (core/update-entity auth-info anything) => [entity target1 target2]
                               (links/add-link auth-info entity auth-user-id rel targetid1) => [entity target1]
                               (links/add-link auth-info entity auth-user-id rel targetid2) => [entity target2]]
            (fact "succeeds with 201"
              (:status (app (request id))) => 201)
            (fact "returns link documents"
              (body-json (request id)) => {:entities [entity target1 target2]
                                           :links    []})
            (fact "=> 401 if not can? update source"
              (:status (app (request id))) => 401
              (provided
                (links/add-link auth-info entity auth-user-id rel anything) =throws=> (sling-throwable {:type :ovation.auth/unauthorized}))))))

      (facts "DELETE /entities/:id/links/:rel"
        (let [targetid (UUID/randomUUID)
              link {:target_id targetid
                    :rel       rel
                    :_id       (UUID/randomUUID)
                    :source_id (UUID/randomUUID)}
              request (fn [id] (mock-req (mock/request :delete (util/join-path ["" "api" ver/version "entities" id "links" rel targetid])) apikey))]

          (against-background [(core/get-entities auth-info [id]) => [entity]
                               (links/delete-link auth-info entity auth-user-id rel (str targetid)) => [link]]
            (fact "succeeds with 202"
              (:status (app (request id))) => 202)
            (fact "deletes link documents"
              (body-json (request id)) => {:links [(assoc link :_id (str (:_id link))
                                                               :source_id (str (:source_id link))
                                                               :target_id (str (:target_id link)))]})
            (fact "=> 401 if not can? update source"
              (:status (app (request id))) => 401
              (provided
                (links/delete-link auth-info entity auth-user-id rel anything) =throws=> (sling-throwable {:type :ovation.auth/unauthorized}))))))
      )))

(defmacro entity-resource-tests
  "Facts about a resource type (e.g. \"Project\")"
  [entity-type]
  (let [type-name (capitalize entity-type)
        type-path (lower-case (str type-name "s"))]
    `(let [apikey# "--apikey--"
           auth-info# {:user "..user.."}]

       (against-background [(auth/authorize anything apikey#) => auth-info#]
         (facts ~(util/join-path ["" type-path])
           (facts "read"
             (let [id# (str (UUID/randomUUID))
                   project# {:_id        id#
                             :_rev       "123"
                             :type       ~type-name
                             :attributes {}
                             :links      {}}]
               (let [get-req# (mock-req (mock/request :get (util/join-path ["" "api" ~ver/version ~type-path])) apikey#)]
                 (against-background [(core/of-type auth-info# ~type-name) => [project#]]
                   (fact ~(str "GET / gets all" type-path)
                     (body-json get-req#) => {~(keyword type-path) [project#]})))

               (let [get-req# (mock-req (mock/request :get (util/join-path ["" "api" ~ver/version ~type-path id#])) apikey#)]
                 (against-background [(core/get-entities auth-info# [id#]) => [project#]]
                   (fact ~(str "GET /:id gets a single " (lower-case type-name))
                     (body-json get-req#) => {~(keyword (lower-case type-name)) project#})
                   (let [source# {:_id        id#
                                  :_rev       "123"
                                  :type       "OtherType"
                                  :attributes {}
                                  :links      {}}]
                     (fact ~(str "GET /:id returns 404 if not a " (lower-case type-name))
                       (:status (app get-req#)) => 404
                       (provided
                         (core/get-entities auth-info# [id#]) => [source#])))))))
           (facts "create"
             (let [parent-id# ~(str (UUID/randomUUID))
                   new-entity# {:type "MyEntity" :attributes {:foo "bar"}}
                   new-entities# [new-entity#]
                   entity# [(assoc new-entity# :_id ~(str (UUID/randomUUID))
                                               :_rev "1")]
                   request# (fn [] (mock-req (-> (mock/request :post (util/join-path ["" "api" ~ver/version ~type-path parent-id#]))
                                               (mock/body (json/write-str (walk/stringify-keys new-entities#)))) apikey#))]

               (against-background [(core/create-entity auth-info# new-entities# :parent parent-id#) => [entity#]]
                 (fact "POST /:id returns status 201"
                   (let [post# (request#)]
                     (:status (app post#)) => 201))
                 (fact ~(str "POST /:id inserts entity with " type-name " parent")
                   (let [post# (request#)]
                     (body-json post#) => {:entities [entity#]})))

               (fact "POST /:id returns 401 if not can? :create"
                 (:status (app (request#))) => 401
                 (provided
                   (core/create-entity auth-info# new-entities# :parent parent-id#) =throws=> (sling-throwable {:type :ovation.auth/unauthorized})))
               )

             ;; For top-level entities
             (when (#{"Project" "Source" "Protocol"} ~(capitalize type-name))
               (let [new-entity# {:type ~(capitalize type-name) :attributes {:foo "bar"}}
                     new-entities# [new-entity#]
                     entity# [(assoc new-entity# :_id ~(str (UUID/randomUUID))
                                                 :_rev "1")]
                     request# (fn [] (mock-req (-> (mock/request :post (util/join-path ["" "api" ~ver/version ~type-path]))
                                                 (mock/body (json/write-str (walk/stringify-keys new-entities#)))) apikey#))]

                 (against-background [(core/create-entity auth-info# new-entities#) => [entity#]]
                   (fact "POST / returns status 201"
                     (let [post# (request#)]
                       (:status (app post#)) => 201))
                   (fact ~(str "POST / inserts new top-level " type-name)
                     (let [post# (request#)]
                       (body-json post#) => {~(keyword type-path) [entity#]}))
                   )

                 (let [bad-entities# [{:type "Other" :attributes {:foo "bar"}}]
                       bad-request# (fn [] (mock-req (-> (mock/request :post (util/join-path ["" "api" ~ver/version ~type-path]))
                                                       (mock/body (json/write-str (walk/stringify-keys bad-entities#)))) apikey#))]
                   (fact "POST / returns 400 if type does not match"
                     (:status (app (bad-request#))) => 400))

                 (fact "POST / returns 401 if not can? :create"
                   (:status (app (request#))) => 401
                   (provided
                     (core/create-entity auth-info# new-entities#) =throws=> (sling-throwable {:type :ovation.auth/unauthorized})))
                 )))

           (facts "update"
             (let [id# (UUID/randomUUID)
                   attributes# {:foo "bar"}
                   entity# {:type       ~type-name
                           :_id        id#
                           :_rev       "1"
                           :attributes attributes#}
                   new-attributes# {:bar "baz"}
                   update# (assoc entity# :attributes new-attributes#)
                   updated-entity# (assoc update# :_rev "2" :links {} :_id (str id#))
                   request# (fn [entity-id#] (mock-req (-> (mock/request :put (util/join-path ["" "api" ~ver/version ~type-path (str entity-id#)]))
                                                       (mock/body (json/write-str (walk/stringify-keys (assoc update# :_id (str id#)))))) apikey#))]

               (against-background [(core/update-entity auth-info# [update#]) => [updated-entity#]]
                 (fact "succeeds with status 200"
                   (let [response# (app (request# id#))]
                     (:status response#) => 200))
                 (fact "updates single entity by ID"
                   (let [request# (request# id#)]
                     (body-json request#)) => {~(keyword type-path) [updated-entity#]})
                 (fact "fails if entity and path :id do not match"
                   (let [other-id# (str (UUID/randomUUID))
                         response# (app (request# other-id#))]
                     (:status response#) => 404)))

               (fact "fails if not can? :update"
                 (:status (app (request# id#))) => 401
                 (provided
                   (core/update-entity auth-info# [update#]) =throws=> (sling-throwable {:type :ovation.auth/unauthorized})))
               )
             )

           (facts "delete"
             (let [id# (UUID/randomUUID)
                   attributes# {:foo "bar"}
                   entity# {:type       "MyEntity"
                           :_id        id#
                           :_rev       "1"
                           :attributes attributes#}
                   deleted-entity# (assoc entity# :transh_info {} :_id (str id#))
                   request# (fn [entity-id#] (mock-req (-> (mock/request :delete (util/join-path ["" "api" ~ver/version ~type-path (str entity-id#)]))) apikey#))]

               (against-background [(core/delete-entity auth-info# [(str id#)]) => [deleted-entity#]]
                 (fact "succeeds with status 202"
                   (let [response# (app (request# id#))]
                     (:status response#) => 202))
                 (fact "DELETE /:id trashes entity"
                   (let [delete-request# (request# id#)]
                     (body-json delete-request#)) => {:entities [deleted-entity#]}))

               (fact "fails if not can? :delete"
                 (:status (app (request# id#))) => 401
                 (provided
                   (core/delete-entity auth-info# [(str id#)]) =throws=> (sling-throwable {:type :ovation.auth/unauthorized}))))))))))


(facts "About named resource types"
  (entity-resource-tests "Project")
  (entity-resource-tests "Source"))
