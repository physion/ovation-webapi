(ns ovation.test.teams
  (:use midje.sweet)
  (:require [ovation.auth :as auth]
            [ovation.teams :as teams]
            [ovation.routes :as routes]
            [org.httpkit.fake :refer [with-fake-http]]
            [ovation.util :as util]
            [ovation.config :as config])
  (:import (clojure.lang ExceptionInfo)))

(facts "About Teams API"
  (facts "get-team*"
    (against-background [(auth/authenticated-user-id ..auth..) => ..user-id..
                         ..request.. =contains=> {:auth/auth-info ..auth..}
                         ..auth.. =contains=> {:api_key ..apikey..}
                         (routes/router ..request..) => ..rt..]
      (let [team-id (str (util/make-uuid))
            team-url (util/join-path [config/TEAMS_SERVER "teams" team-id])]
        (fact "should return existing team"
          (with-fake-http [team-url {:status 200
                                     :body   (util/to-json {:team {:id          team-id
                                                                   :memberships []}})}]
            (teams/get-team* ..request.. team-id) => {:team {:id          team-id
                                                             :memberships []
                                                             :links       {:self  ..self-url..
                                                                           :roles ..roles-url..}}}
            (provided
              (routes/named-route ..rt.. :get-team {:id team-id}) => ..self-url..
              (routes/named-route ..rt.. :all-roles {:id team-id}) => ..roles-url..)))

        (fact "should throw not-found! for non-existant team"
          (with-fake-http [team-url {:status 404}]
            (teams/get-team* ..request.. team-id)) => (throws ExceptionInfo)))))

  (facts "get-memberships*"
    (against-background [(auth/authenticated-user-id ..auth..) => ..user-id..
                         ..request.. =contains=> {:auth/auth-info ..auth..}
                         ..auth.. =contains=> {:api_key ..apikey..}
                         (routes/router ..request..) => ..rt..]
      (let [team-id (str (util/make-uuid))
            memberships-url (util/join-path [config/TEAMS_SERVER "teams" team-id "memberships"])]
        (fact "returns team Memberships"
          (with-fake-http [memberships-url {:status 200
                                            :body   (util/to-json {:memberships []})}]
            (teams/get-memberships* ..request.. team-id) => {:memberships []}))))))
