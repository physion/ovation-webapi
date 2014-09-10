(ns ovation-rest.test.test_handler
    (:require [midje.sweet :refer :all]
              [ring.mock.request :refer :all]
              [ovation-rest.handler :as handler]))

(facts "about doc route"
       (fact "HEAD /index.html returns 200"
             (let [response (handler/app (request :head "/index.html"))]
                  (:status response) => 200)))

(facts "about invalid routes"
       (fact "returns 404 for invalid path"
             (let [response (handler/app (request :get "/invalid/path?api-key=123"))]
                  (:status response) => 404)))
