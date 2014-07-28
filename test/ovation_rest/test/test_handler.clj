(ns ovation-rest.test.test_handler
  (:require [midje.sweet :refer :all]
            [ring.mock.request :refer :all]
            [ovation-rest.handler :as handler]))

(facts "about main route"
       (fact "returns 200"
             (let [response (handler/app (request :get "/"))]
               (:status response) => 200))
       (fact "body is \"Hello World\""
             (let [response (handler/app (request :get "/"))]
               (:body response) => "Hello World")))

  (facts "about invalid routes"
         (fact "returns 404"
               (let [response (handler/app (request :get "/invalid"))]
                 (:status response) => 404)))
