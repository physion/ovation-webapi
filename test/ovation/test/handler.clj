(ns ovation.test.handler
  (:import (java.util UUID)
           (us.physion.ovation.exceptions AuthenticationException))
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [ovation.handler :as handler]
            [clojure.data.json :as json]
            [ovation.util :as util]
            [ovation.entity :as entity]
            [clojure.walk :as walk]
            [ovation.links :as links]
            [ovation.context :as ctx]
            [slingshot.slingshot :refer [try+]]
            ))

(facts "About authorization"
       (fact "invalid API key returns 401"
             (let [apikey "12345"
                   get (-> (mock/request :get "/api/v1/entities/123")
                           (mock/query-string {"api-key" apikey})
                           (mock/content-type "application/json"))]
               (:status (handler/app get)) => 401
               (provided
                 (#'ovation.context/make-server-helper anything anything) =throws=> (AuthenticationException. "Crap")))))


(facts "About doc route"
       (fact "HEAD / => 302"
             (let [response (handler/app (mock/request :head "/"))]
               (:status response) => 302))
       (fact "GET / redirects"
             (let [response (handler/app (mock/request :get "/"))]
               (:status response) => 302
               (-> response (:headers) (get "Location")) => "/index.html"))
       (fact "HEAD /index.html returns 200"
             (let [response (handler/app (mock/request :head "/index.html"))]
               (:status response) => 200)))

(facts "About invalid routes"
       (fact "returns 404 for invalid path"
             (let [response (handler/app (mock/request :get "/invalid/path"))]
               response => nil?)))

(facts "About entities"
       (fact "POST /entities inserts an entity (201)"
             (let [dto {:type       "Project"
                        :attributes {}}
                   post (-> (mock/request :post "/api/v1/entities")
                            (mock/query-string {"api-key" "12345"})
                            (mock/content-type "application/json")
                            (mock/body (json/write-str (walk/stringify-keys dto))))]
               (:status (handler/app post)) => 201
               (provided
                 (entity/create-entity "12345" {:type       "Project"
                                                :attributes {}}) => [{:_id        "id"
                                                                      :_rev       "rev"
                                                                      :attributes {}
                                                                      :type       "Project"}])))

       (fact "PUT /entities/:id updates the attributes in entity (200)"
             (let [uuid (UUID/randomUUID)
                   id (str uuid)
                   dto {:type       "Project"
                        :attributes {"attr1" 1
                                     "attr2" "value"}
                        :_id        id
                        :_rev       "rev"}]

               (:status (handler/app (-> (mock/request :put "/api/v1/entities/123")
                                         (mock/query-string {"api-key" "12345"})
                                         (mock/content-type "application/json")
                                         (mock/body (json/write-str (walk/stringify-keys dto)))))) => 200
               (provided
                 (entity/update-entity-attributes "12345" "123" {:attr1 1
                                                                 :attr2 "value"}) => [{:_id        (str uuid)
                                                                                       :_rev       "rev2"
                                                                                       :attributes {"attr1" 1}
                                                                                       :links      {}
                                                                                       :type       "Project"}])))

       (fact "POST /entities/:id/links creates a new link (201)"
             (let [target_uuid (UUID/randomUUID)
                   project_uuid (UUID/randomUUID)
                   project_id (str project_uuid)
                   target_id (str target_uuid)
                   link {:target_id   target_id
                         :rel         "patients"
                         :inverse_rel "projects"}
                   body (json/write-str (walk/stringify-keys link))
                   path (str "/api/v1/entities/" project_id "/links")
                   apikey "12345"]

               (:status (handler/app (-> (mock/request :post path)
                                         (mock/query-string {"api-key" apikey})
                                         (mock/content-type "application/json")
                                         (mock/body body)))) => 201
               (provided
                 (links/create-link apikey project_id {:target_id target_uuid
                                                       :rel "patients"
                                                       :inverse_rel "projects"}) => {:success true})))
       )
