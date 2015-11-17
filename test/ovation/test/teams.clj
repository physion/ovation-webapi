(ns ovation.test.teams
  (:use midje.sweet)
  (:require [ovation.auth :as auth]
            [ovation.teams :as teams]
            [ovation.routes :as routes]
            [org.httpkit.fake :refer [with-fake-http]]
            [ovation.util :as util]
            [ovation.config :as config]
            [ovation.core :as core])
  (:import (clojure.lang ExceptionInfo)))

(facts "About Teams API"
  (facts "get-team*"
    (against-background [(auth/authenticated-user-id ..auth..) => ..user-id..
                         ..request.. =contains=> {:auth/auth-info ..auth..}
                         ..auth.. =contains=> {:api_key ..apikey..}
                         (routes/router ..request..) => ..rt..]
      (let [team-id (str (util/make-uuid))
            user-id (str (util/make-uuid))
            team-url (util/join-path [config/TEAMS_SERVER "teams" team-id])
            rails-team {:team {:id                  1
                               :name                team-id
                               :uuid                team-id
                               :organization        {}
                               :project             {}
                               :roles               []
                               :pending_memberships [{
                                                      :id        232,
                                                      :role_name "Administrator'",
                                                      :email     "newmember@example.com"
                                                      },
                                                     {
                                                      :id        2323,
                                                      :role_name "Member",
                                                      :email     "newmember@example.com"
                                                      }]
                               :memberships         [{:id      3232
                                                      :team_id 1
                                                      :added   "2015-02-01"
                                                      :role_id 21
                                                      :user    {
                                                                :id    3
                                                                :uuid  user-id
                                                                :name  "Bob"
                                                                :email "bob@example.com"
                                                                :links {:roles "..."}
                                                                }
                                                      :links   {:membership_roles ""}}]}}
            expected {:team {:id                  1
                             :type                "Team"
                             :name                team-id
                             :uuid                team-id
                             :roles               []
                             :pending_memberships [{
                                                    :id        232,
                                                    :role_name "Administrator'",
                                                    :email     "newmember@example.com"
                                                    },
                                                   {
                                                    :id        2323,
                                                    :role_name "Member",
                                                    :email     "newmember@example.com"
                                                    }]
                             :memberships         [{:id      3232
                                                    :team_id 1
                                                    :added   "2015-02-01"
                                                    :role_id 21
                                                    :user    {
                                                              :id    3
                                                              :uuid  user-id
                                                              :name  "Bob"
                                                              :email "bob@example.com"
                                                              :links {:roles "..."}
                                                              }
                                                    :links   {:membership_roles ""}}]
                             :links               {:self        ..self-url..
                                                   :memberships ..membership-url..}}}]

        (fact "should return existing team"
          (with-fake-http [team-url {:status 200
                                     :body   (util/to-json rails-team)}]

            (teams/get-team* ..request.. team-id) => expected
            (provided
              (routes/named-route ..rt.. :get-team {:id team-id}) => ..self-url..
              (routes/named-route ..rt.. :post-memberships {:id team-id}) => ..membership-url..)))

        (fact "should return nil for missing team when :allow-nil true"
          (with-fake-http [team-url {:status 404}]
            (teams/get-team* ..request.. team-id :allow-nil true) => nil))

        (fact "should throw! other response codes"
          (with-fake-http [team-url {:status 401}]
            (teams/get-team* ..request.. team-id) => (throws ExceptionInfo)))

        (fact "should throw not-found! for missing team wne :allow-nil false"
          (with-fake-http [team-url {:status 404}]
            (teams/get-team* ..request.. team-id) => (throws ExceptionInfo))))))


  (facts "post-memberhsip*"
    (against-background [(auth/authenticated-user-id ..auth..) => ..user-id..
                         ..request.. =contains=> {:auth/auth-info ..auth..}
                         ..auth.. =contains=> {:api_key ..apikey..}
                         (routes/router ..request..) => ..rt..]
      (let [team-id (str (util/make-uuid))
            team-url (util/join-path [config/TEAMS_SERVER "teams" team-id])
            teams-url (util/join-path [config/TEAMS_SERVER "teams"])
            memberships-url (util/join-path [config/TEAMS_SERVER "teams" team-id "memberships"])
            membership-id 1
            membership-url (util/join-path [memberships-url membership-id])
            team {:team {:id          team-id
                         :memberships []}}
            user-email "example@example.com"
            membership {:membership {:team_id team-id
                                     :email   user-email
                                     :role_id 1
                                     :links   {:self membership-url}}}]

        (against-background [(routes/named-route ..rt.. :put-membership {:id team-id :mid membership-id}) => membership-url
                             (routes/named-route ..rt.. :get-team {:id team-id}) => team-url
                             (routes/named-route ..rt.. :post-memberships {:id team-id}) => memberships-url]
          (fact "creates membership for existing team"
            (with-fake-http [team-url {:status 200
                                       :body   (util/to-json team)}
                             {:url memberships-url :method :post} {:status 201
                                                                   :body   (util/to-json {:membership {:id      membership-id
                                                                                                       :team_id team-id
                                                                                                       :email   user-email
                                                                                                       :role_id 1}})}]
              (teams/post-membership* ..request.. team-id membership) =>  (assoc-in membership [:membership :id] membership-id)))

          (fact "creates new team on first membership"
            (with-fake-http [{:url team-url :method :get} {:status 404}
                             {:url memberships-url :method :post} {:status 201
                                                                   :body   (util/to-json {:membership {:id      1
                                                                                                       :team_id team-id
                                                                                                       :email   user-email
                                                                                                       :role_id 1}})}]
              (teams/post-membership* ..request.. team-id membership) => (assoc-in membership [:membership :id] membership-id)
              (provided
                (teams/get-team* ..request.. team-id :allow-nil false) => nil
                (teams/create-team ..request.. team-id) => true)))))))

  (facts "create-team"
    (against-background [(auth/authenticated-user-id ..auth..) => ..user-id..
                         ..request.. =contains=> {:auth/auth-info ..auth..}
                         ..auth.. =contains=> {:api_key ..apikey..}
                         (routes/router ..request..) => ..rt..
                         (routes/named-route ..rt.. :post-teams {}) => teams-url]
      (let [team-id (str (util/make-uuid))
            teams-url (util/join-path [config/TEAMS_SERVER "teams"])
            team {:team {:id          team-id
                         :memberships []}}]
        (fact "creates team"
          (with-fake-http [{:url teams-url :method :post} {:status 201
                                                           :body   (util/to-json team)}]
            (teams/create-team ..request.. team-id) => team))

        (fact "throws! responses not 201"
          (with-fake-http [{:url teams-url :method :post} {:status 401}]
            (teams/create-team ..request.. team-id) => (throws ExceptionInfo))))))

  (facts "put-membership*"
    (future-fact "updates memberhsip role")))