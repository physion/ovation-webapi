(ns ovation.test.tokens
  (:use midje.sweet)
  (:require [ovation.tokens :as tokens]
            [org.httpkit.fake :refer [with-fake-http]]
            [clojure.data.json :as json]
            [ovation.util :as util]
            [ovation.auth :as auth]))


(facts "About tokens services"
  (facts "password grant"
    (fact "proxies call to services"
      (with-fake-http [{:url tokens/auth-service-url
                        :method :post} (json/write-str {:token "abc123"})]
        (:status (tokens/get-token "email" "pw")) => 200
        (:body (tokens/get-token "email" "pw")) => {:token "abc123"})))
  (facts "refresh"
    (fact "proxies call to services"
      (with-fake-http [{:url    (util/join-path [tokens/auth-service-url "refresh"])
                        :method :post} (json/write-str {:token "abc123"})]
        (:status (tokens/refresh-token ..req..)) => 200
        (:body (tokens/refresh-token ..req..)) => {:token "abc123"}
        (provided
          (auth/identity ..req..) => {::auth/token "current-token"})))))

