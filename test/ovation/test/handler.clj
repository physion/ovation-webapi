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
            [ovation.links :as links])
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

;(facts "About Swagger API"
;  (fact "is valid"
;    (compojure.api.swagger/validate app) => nil))

(facts "About invalid routes"
  (let [apikey "..apikey.."
        auth-response (promise)
        _ (deliver auth-response {:status 200 :body "{\"user\": \"..user..\"}"})]
    (against-background [(auth/get-auth anything apikey) => auth-response]
      (fact "invalid path =>  404"
        (let [response (app (mock-req (mock/request :get "/invalid/path") apikey))]
          response => nil?)))))

(facts "About named resource types"
  (let [apikey "..apikey.."
        auth-response (promise)
        auth-info {:user "..user.."}
        _ (deliver auth-response {:status 200 :body (util/to-json auth-info)})]

    (against-background [(auth/get-auth anything apikey) => auth-response]
      (facts "/projects"
        (future-fact "GET / gets all projects")
        (future-fact "GET / returns only projects")
        (future-fact "GET /:id gets a single project")
        (future-fact "POST /:id inserts entities with Project parent")))))


(defn body-json
  [request]
  (let [response (app request)
        reader (clojure.java.io/reader (:body response))
        result (json/read reader)]
    (walk/keywordize-keys result)))


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

          (fact "returns 201 if not can? :create"
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
                :links {}}]
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
              (body-json (request id)) => {:entities [entity target1 target2]}))))
      )))
