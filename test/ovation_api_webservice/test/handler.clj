(ns ovation-api-webservice.test.handler
  (:use clojure.test
        midje.sweet
        ring.mock.request
        ovation-api-webservice.handler))

(deftest test-app-main-route
  (facts "about main route"
         (fact "returns 200"
               (let [response (app (request :get "/"))]
                 (:status response) => 200))
         (fact "body is \"Hello World\""
               (let [response (app (request :get "/"))]
                 (:body response) => "Hello World"))))

(deftest test-app-invalid-route
  (facts "about invalid routes"
         (fact "returns 404"
               (let [response (app (request :get "/invalid"))]
                 (:status response) => 404))))
