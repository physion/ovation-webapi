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
            [schema.core :as s])
  (:import (java.util UUID)))

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
              get (mock-req (mock/request :get (str "/api/v1/entities/" id)) apikey)
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
            (future-fact "succeeds with status 200"
              (let [response (app (request id))]
                (:status response) => 200))
            (fact "updates single entity by ID"
              (let [request (request id)]
                (body-json request)) => {:entities [updated-entity]})
            (fact "fails if entity and path :id do not match"
              (let [other-id (str (UUID/randomUUID))
                    response (app (request other-id))]
                (:status response) => 404))

              (future-fact "fails if not can? :update")))
        )

      (facts "create"
        (let [parent-id (str (UUID/randomUUID))
              new-entity {:type "MyEntity" :attributes {:foo "bar"}}
              new-entities [new-entity]
              entity [(assoc new-entity :_id (str (UUID/randomUUID))
                                        :_rev "1")]]

          (against-background [(core/create-entity auth-info new-entities :parent parent-id) => [entity]]
            (fact "POST /:id inserts entities with parent"
              (let [post (mock-req (-> (mock/request :post (str "/api/" ver/version "/entities/" parent-id))
                                     (mock/body (json/write-str (walk/stringify-keys new-entities)))) apikey)]
                (:status (app post)) => 201))
            (fact "POST /:id inserts entity with parent"
              (let [post (mock-req (-> (mock/request :post (str "/api/" ver/version "/entities/" parent-id))
                                     (mock/body (json/write-str (walk/stringify-keys new-entities)))) apikey)]
                (body-json post) => {:entities [entity]})))
          ))


      (facts "delete"
        (future-fact "DELETE /:id deletes entity")
        (future-fact "fails if not can? :delete"))
      )))
