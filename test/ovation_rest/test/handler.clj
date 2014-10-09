(ns ovation-rest.test.handler
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [ovation-rest.handler :as handler]
            [clojure.data.json :as json]
            [ovation-rest.util :as util]
            [ovation-rest.entity :as entity]
            [clojure.walk :as walk]))

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
                                                             :type       "Project"}]))))
