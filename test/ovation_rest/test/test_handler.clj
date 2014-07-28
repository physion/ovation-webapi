(ns ovation-rest.test.test_handler
  (:require [midje.sweet :refer :all]
            [ring.mock.request :refer :all]
            [ovation-rest.handler :as handler]))

(facts "about main route"
       (fact "returns 200"
             (let [response (handler/app (request :get "/"))]
               (:status response) => 200))
       (fact "body is \"Ovation REST API\""
             (let [response (handler/app (request :get "/"))]
               (:body response) => "Ovation REST API")))

  (facts "about invalid routes"
         (fact "returns 404 for invalid path"
               (let [response (handler/app (request :get "/invalid/path?api-key=123"))]
                 (:status response) => 404)))
