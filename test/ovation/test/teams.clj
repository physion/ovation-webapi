(ns ovation.test.teams
  (:use midje.sweet)
  (:require [ovation.auth :as auth]
            [ovation.teams :as teams]
            [ovation.routes :as routes]
            [org.httpkit.fake :refer [with-fake-http]]
            [ovation.util :as util])
  (:import (clojure.lang ExceptionInfo)))

(facts "About Teams API"
  (facts "get-team*"
    (against-background [(auth/authenticated-user-id ..auth..) => ..user-id..
                         ..request.. =contains=> {:auth/auth-info ..auth..}
                         ..auth.. =contains=> {:api_key ..apikey..}
                         (routes/router ..request..) => ..rt..]
      (fact "should return existing team"
        (teams/get-team* ..request.. ..id..) => {:team {:id          ..id..
                                                        :memberships []
                                                        :links       {:self ..self-url..}}}
        (provided
          (routes/named-route ..rt.. :get-team {:id ..id..}) => ..self-url..))

      (fact "should throw not-found! for non-existant team"
        (with-fake-http [] {:status 404}
          (teams/get-team* ..request.. ..id..)) => (throws ExceptionInfo))))

  (facts "get-roles*"
    (against-background [..request.. =contains=> {:auth/auth-info ..auth..}
                         ..auth.. =contains=> {:api_key ..apikey..}]
      (fact "should return organization roles"
        (with-fake-http ["https://dev.ovation.io/api/v1/roles" {:status 200
                                                                :body   (util/to-json {:roles ["role1" "role2"]})}]
          (teams/get-roles* ..request..)) => {:roles ["role1" "role2"]}))))
