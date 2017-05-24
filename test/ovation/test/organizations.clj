(ns ovation.test.organizations
  (:use midje.sweet)
  (:require [ovation.organizations :as orgs]
            [clojure.core.async :as core.async :refer [chan]]
            [ovation.util :as util :refer [<??]]
            [org.httpkit.fake :refer [with-fake-http]]
            [ovation.config :as config]
            [ovation.request-context :as request-context]
            [ovation.routes :as routes]
            [ovation.test.helpers :refer [sling-throwable]]
            [slingshot.slingshot :refer [try+]])
  (:import (clojure.lang ExceptionInfo)))

(facts "groups-memberships"
  (let [id               1
        user-id          10
        org-id           3
        group-id         21
        service-url      (util/join-path [config/SERVICES_API "api" "v2"])
        rails-membership {"id"              id
                          "user_id"         user-id
                          "organization_id" org-id}]
    (against-background [(request-context/token ..ctx..) => ..auth..
                         ..ctx.. =contains=> {::request-context/auth    ..auth..
                                              ::request-context/routes  ..rt..
                                              ::request-context/org     org-id
                                              ::request-context/request {:params {:id group-id}}}
                         (request-context/router ..request..) => ..rt..]
      (facts "`get-memberships`"
        (with-fake-http [{:url (util/join-path [service-url orgs/GROUP-MEMBERSHIPS]) :method :get} (fn [_ {query-params :query-params} _]
                                                                                                     (if (= group-id (:organization_group_id query-params))
                                                                                                       {:status 200
                                                                                                        :body   (util/to-json {:organization_group_memberships [rails-membership]})}
                                                                                                       {:status 422
                                                                                                        :body   "{}"}))]
          (fact "proxies service response"
            (let [c              (chan)
                  expected-group {:id              id
                                  :type            "GroupMembership"
                                  :user_id         user-id
                                  :organization_id org-id
                                  :links           {:self {:id group-id, :membership-id id, :org org-id}}}]
              (orgs/get-group-memberships ..ctx.. service-url group-id c)
              (<?? c) => [expected-group]))))

      (facts "`get-membership`"
        (with-fake-http [{:url (util/join-path [service-url orgs/GROUP-MEMBERSHIPS id]) :method :get} {:status 200
                                                                                                       :body   (util/to-json {:organization_group_membership rails-membership})}]
          (fact "proxies service response"
            (let [c                   (chan)
                  expected-membership {:id              id
                                       :type            "GroupMembership"
                                       :user_id         user-id
                                       :organization_id org-id
                                       :links           {:self {:id group-id, :membership-id id :org org-id}}}]
              (orgs/get-group-membership ..ctx.. service-url id c)
              (<?? c) => expected-membership))))

      (facts "`create-group-membership`"
        (with-fake-http [{:url (util/join-path [service-url orgs/GROUP-MEMBERSHIPS]) :method :post} (fn [_ {body :body} _]
                                                                                                      (if (= {:organization_group_membership {:type "GroupMembership"
                                                                                                                                 :user_id           user-id
                                                                                                                                 :organization_id   org-id}} (util/from-json body))
                                                                                                        (let [result {:organization_group_membership {:id id
                                                                                                                                         :user_id         user-id
                                                                                                                                         :organization_id org-id}}]
                                                                                                          {:status 201
                                                                                                           :body   (util/to-json result)})
                                                                                                        {:status 422}))]
          (fact "proxies service response"
            (let [c        (chan)
                  expected {:id              id
                            :type            "GroupMembership"
                            :user_id         user-id
                            :organization_id org-id
                            :links           {:self {:id group-id, :membership-id id :org org-id}}}
                  new      {:type            "GroupMembership"
                            :user_id         user-id
                            :organization_id org-id}]
              (orgs/create-group-membership ..ctx.. service-url new c)
              (<?? c) => expected))))

      (facts "`update-group`"
        (with-fake-http [{:url (util/join-path [service-url orgs/GROUP-MEMBERSHIPS id]) :method :put} (fn [_ {body :body} _]
                                                                                                        (if (= {:organization_group_membership {:id id
                                                                                                                                   :type            "GroupMembership"
                                                                                                                                   :user_id         user-id
                                                                                                                                   :organization_id org-id}} (util/from-json body))
                                                                                                          (let [result {:organization_group_membership {:id id
                                                                                                                                           :user_id         user-id
                                                                                                                                           :organization_id org-id}}]
                                                                                                            {:status 200
                                                                                                             :body   (util/to-json result)})
                                                                                                          {:status 422}))]
          (fact "proxies service response"
            (let [c        (chan)
                  expected {:id              id
                            :type            "GroupMembership"
                            :user_id         user-id
                            :organization_id org-id
                            :links           {:self {:id group-id, :membership-id id, :org org-id}}}
                  updated  {:id              id
                            :type            "GroupMembership"
                            :user_id         user-id
                            :organization_id org-id}]
              (orgs/update-group-membership ..ctx.. service-url id updated c)
              (<?? c) => expected))))

      (facts "`delete-group`"
        (facts "with 204"
          (with-fake-http [{:url (util/join-path [service-url orgs/GROUP-MEMBERSHIPS id]) :method :delete} {:status 204 :body "{}"}]
            (fact "proxies service response"
              (let [c (chan)]
                (orgs/delete-group-membership ..ctx.. service-url id c)
                (<?? c) => {}))))
        (facts "with 200"
          (with-fake-http [{:url (util/join-path [service-url orgs/GROUP-MEMBERSHIPS id]) :method :delete} {:status 200 :body "{}"}]
            (fact "proxies service response"
              (let [c (chan)]
                (orgs/delete-group-membership ..ctx.. service-url id c)
                (<?? c) => {}))))))))

(facts "organization-groups"
  (let [id          1
        user-id     10
        org-id      3
        service-url (util/join-path [config/SERVICES_API "api" "v2"])
        rails-group {"id"              id
                     "user_id"         user-id
                     "organization_id" org-id}]
    (against-background [(request-context/token ..ctx..) => ..auth..
                         ..ctx.. =contains=> {::request-context/auth   ..auth..
                                              ::request-context/routes ..rt..
                                              ::request-context/org    org-id}
                         (request-context/router ..request..) => ..rt..]
      (facts "`get-groups`"
        (with-fake-http [{:url (util/join-path [service-url orgs/ORGANIZATION-GROUPS]) :method :get} {:status 200
                                                                                                      :body   (util/to-json {:organization_groups [rails-group]})}]
          (fact "proxies service response"
            (let [c              (chan)
                  expected-group {:id              id
                                  :type            "OrganizationGroup"
                                  :user_id         user-id
                                  :organization_id org-id
                                  :links           {:self              {:id id, :org org-id}
                                                    :group-memberships {:id id, :org org-id}}}]
              (orgs/get-groups ..ctx.. service-url c)
              (<?? c) => [expected-group]))))

      (facts "`get-group`"
        (with-fake-http [{:url (util/join-path [service-url orgs/ORGANIZATION-GROUPS id]) :method :get} {:status 200
                                                                                                         :body   (util/to-json {:organization_group rails-group})}]
          (fact "proxies service response"
            (let [c              (chan)
                  expected-group {:id              id
                                  :type            "OrganizationGroup"
                                  :user_id         user-id
                                  :organization_id org-id
                                  :links           {:self              {:id id, :org org-id}
                                                    :group-memberships {:id id, :org org-id}}}]
              (orgs/get-group ..ctx.. service-url id c)
              (<?? c) => expected-group))))

      (facts "`create-group`"
        (with-fake-http [{:url (util/join-path [service-url orgs/ORGANIZATION-GROUPS]) :method :post} (fn [_ {body :body} _]
                                                                                                        (if (= {:organization_group {:organization_id org-id
                                                                                                                                     :user_id         user-id}} (util/from-json body))
                                                                                                          (let [result {:organization_group {:id              id
                                                                                                                                             :organization_id org-id
                                                                                                                                             :user_id         user-id}}]
                                                                                                            {:status 201
                                                                                                             :body   (util/to-json result)})
                                                                                                          {:status 422}))]
          (fact "proxies service response"
            (let [c        (chan)
                  expected {:id              id
                            :type            "OrganizationGroup"
                            :user_id         user-id
                            :organization_id org-id
                            :links           {:self              {:id id, :org org-id}
                                              :group-memberships {:id id, :org org-id}}}
                  new      {:user_id         user-id
                            :organization_id org-id}]
              (orgs/create-group ..ctx.. service-url new c)
              (<?? c) => expected))

          (fact "raises 422 for organizaiton mismatch"
            (let [c   (chan)
                  new {:user_id         user-id
                       :organization_id "other-org"}]
              (orgs/create-group ..ctx.. service-url new c)
              (<?? c) => (throws ExceptionInfo)))))

      (facts "`update-group`"
        (with-fake-http [{:url (util/join-path [service-url orgs/ORGANIZATION-GROUPS id]) :method :put} (fn [_ {body :body} _]
                                                                                                          (if (= {:organization_group {:id              id
                                                                                                                                       :type            "OrganizationGroup"
                                                                                                                                       :organization_id org-id
                                                                                                                                       :user_id         user-id}} (util/from-json body))
                                                                                                            (let [result {:organization_group {:id              id
                                                                                                                                               :organization_id org-id
                                                                                                                                               :user_id         user-id}}]
                                                                                                              {:status 200
                                                                                                               :body   (util/to-json result)})
                                                                                                            {:status 422}))]
          (fact "proxies service response"
            (let [c        (chan)
                  expected {:id              id
                            :type            "OrganizationGroup"
                            :user_id         user-id
                            :organization_id org-id
                            :links           {:self              {:id id, :org org-id}
                                              :group-memberships {:id id, :org org-id}}}
                  updated  {:id              id
                            :type            "OrganizationGroup"
                            :user_id         user-id
                            :organization_id org-id}]
              (orgs/update-group ..ctx.. service-url id updated c)
              (<?? c) => expected))

          (fact "raises 422 for organization mismatch"
            (let [c       (chan)
                  updated {:id              id
                           :type            "OrganizationGroup"
                           :user_id         user-id
                           :organization_id "other-org"}]
              (orgs/update-group ..ctx.. service-url id updated c)
              (<?? c) => (throws ExceptionInfo)))))

      (facts "`delete-group`"
        (facts "with 204"
          (with-fake-http [{:url (util/join-path [service-url orgs/ORGANIZATION-GROUPS id]) :method :delete} {:status 204 :body "{}"}]
            (fact "proxies service response"
              (let [c (chan)]
                (orgs/delete-group ..ctx.. service-url id c)
                (<?? c) => {}))))
        (facts "with 200"
          (with-fake-http [{:url (util/join-path [service-url orgs/ORGANIZATION-GROUPS id]) :method :delete} {:status 200 :body "{}"}]
            (fact "proxies service response"
              (let [c (chan)]
                (orgs/delete-group ..ctx.. service-url id c)
                (<?? c) => {}))))))))

(facts "organization-memberships"
  (let [id               1
        user-id          10
        user-email       "user@example.com"
        org-id           3
        service-url      (util/join-path [config/SERVICES_API "api" "v2"])
        rails-membership {"id"              id
                          "user_id"         user-id
                          "email"           user-email
                          "organization_id" org-id}]
    (against-background [(request-context/token ..ctx..) => ..auth..
                         ..ctx.. =contains=> {::request-context/auth   ..auth..
                                              ::request-context/routes ..rt..
                                              ::request-context/org    org-id}
                         (request-context/router ..request..) => ..rt..]
      (facts "`get-memberships`"
        (with-fake-http [{:url (util/join-path [service-url "organization_memberships"]) :method :get} {:status 200
                                                                                                        :body   (util/to-json {:organization_memberships [rails-membership]})}]
          (fact "proxies service response"
            (let [c                   (chan)
                  expected-membership {:id              id
                                       :type            "OrganizationMembership"
                                       :organization_id org-id
                                       :email           user-email
                                       :links           {:self {:id id, :org org-id}}}]
              (orgs/get-memberships ..ctx.. service-url c)
              (<?? c) => [expected-membership]))))

      (facts "`get-membership`"
        (with-fake-http [{:url (util/join-path [service-url "organization_memberships" id]) :method :get} {:status 200
                                                                                                           :body   (util/to-json {:organization_membership rails-membership})}]
          (fact "proxies service response"
            (let [c                   (chan)
                  expected-membership {:id              id
                                       :type            "OrganizationMembership"
                                       :email user-email
                                       :organization_id org-id
                                       :links           {:self {:id id, :org org-id}}}]
              (orgs/get-membership ..ctx.. service-url id c)
              (<?? c) => expected-membership))))

      (facts "`create-membership`"
        (with-fake-http [{:url (util/join-path [service-url orgs/ORGANIZATION-MEMBERSHIPS]) :method :post} (fn [_ {body :body} _]
                                                                                                             (if (= {:organization_membership {:organization_id org-id
                                                                                                                                               :email           user-email}} (util/from-json body))
                                                                                                               (let [result {:organization_membership {:id              id
                                                                                                                                                       :organization_id org-id
                                                                                                                                                       :email           user-email}}]
                                                                                                                 {:status 201
                                                                                                                  :body   (util/to-json result)})
                                                                                                               {:status 422}))]
          (fact "proxies service response"
            (let [c              (chan)
                  expected       {:id              id
                                  :type            "OrganizationMembership"
                                  :email user-email
                                  :organization_id org-id
                                  :links           {:self {:id id, :org org-id}}}
                  new-membership {:email user-email
                                  :organization_id org-id}]
              (orgs/create-membership ..ctx.. service-url new-membership c)
              (<?? c) => expected))

          (fact "throws 422 for org mismatch"
            (let [c              (chan)
                  new-membership {:email user-email
                                  :organization_id (+ org-id 1)}]
              (orgs/create-membership ..ctx.. service-url new-membership c)
              (<?? c) => (throws ExceptionInfo)))))

      (facts "`update-membership`"
        (with-fake-http [{:url (util/join-path [service-url orgs/ORGANIZATION-MEMBERSHIPS id]) :method :put} (fn [_ {body :body} _]
                                                                                                               (if (= {:organization_membership {:id              id
                                                                                                                                                 :type            "OrganizationMembership"
                                                                                                                                                 :organization_id org-id
                                                                                                                                                 :email           user-email}} (util/from-json body))
                                                                                                                 (let [result {:organization_membership {:id              id
                                                                                                                                                         :organization_id org-id
                                                                                                                                                         :email           user-email}}]
                                                                                                                   {:status 200
                                                                                                                    :body   (util/to-json result)})
                                                                                                                 {:status 422}))]
          (fact "proxies service response"
            (let [c                  (chan)
                  expected           {:id              id
                                      :type            "OrganizationMembership"
                                      :email           user-email
                                      :organization_id org-id
                                      :links           {:self {:id id, :org org-id}}}
                  updated-membership {:id              id
                                      :type            "OrganizationMembership"
                                      :email           user-email
                                      :organization_id org-id}]
              (orgs/update-membership ..ctx.. service-url id updated-membership c)
              (<?? c) => expected))

          (fact "throws 422 for organization id mismatch"
            (let [c                  (chan)
                  updated-membership {:id              id
                                      :type            "OrganizationMembership"
                                      :user_id         user-id
                                      :organization_id (+ 1 org-id)}]
              (orgs/update-membership ..ctx.. service-url id updated-membership c)
              (<?? c) => (throws ExceptionInfo)))))

      (facts "`delete-membership`"
        (facts "with 204"
          (with-fake-http [{:url (util/join-path [service-url "organization_memberships" id]) :method :delete} {:status 204 :body "{}"}]
            (fact "proxies service response"
              (let [c (chan)]
                (orgs/delete-membership ..ctx.. service-url id c)
                (<?? c) => {}))))
        (facts "with 200"
          (with-fake-http [{:url (util/join-path [service-url "organization_memberships" id]) :method :delete} {:status 200 :body "{}"}]
            (fact "proxies service response"
              (let [c (chan)]
                (orgs/delete-membership ..ctx.. service-url id c)
                (<?? c) => {}))))))))

(facts "organizations API"
  (let [rails-org-1 {
                     "id"         1,
                     "uuid"       "1db71db5-9399-4ccb-b7b2-592ae25a810f",
                     "name"       "Name 1",
                     "owner_name" "Barry Wark",
                     "is_admin"   true}
        rails-org-2 {
                     "id"         2,
                     "uuid"       "1db71db5-9399-4ccb-b7b2-592ae25a810f",
                     "name"       "Name 2",
                     "owner_name" "Barry Wark",
                     "is_admin"   true}
        org-name    "My Org"
        new-org     {
                     "id"                       1,
                     "uuid"                     "1db71db5-9399-4ccb-b7b2-592ae25a810f",
                     "name"                     "Name 1",
                     "owner_name"               "Barry Wark",
                     "research_subscription_id" 3,
                     "is_admin"                 true}]



    (against-background [(request-context/token ..ctx..) => ..auth..
                         ..ctx.. =contains=> {::request-context/auth   ..auth..
                                              ::request-context/routes ..rt..}
                         (request-context/router ..request..) => ..rt..]

      (let [orgs-url (util/join-path [config/ORGS_SERVER "organizations"])]

        (facts "POST /o"
          (let [c        (chan)
                org-id   1
                org-url  (util/join-path [config/ORGS_SERVER "organizations"])
                expected {:id 1, :is_admin true, :links {:organization-groups ..groups1.., :organization-memberships ..members1.., :projects ..projects1.., :self ..self1..}, :name "Name 1", :research_subscription_id 3, :type "Organization"}]
            (facts "with success"
              (against-background [(routes/self-route ..ctx.. "organization" org-id 1) => ..self1..
                                   (routes/org-projects-route ..rt.. org-id) => ..projects1..
                                   (routes/org-memberships-route ..rt.. org-id) => ..members1..
                                   (routes/org-groups-route ..rt.. 1) => ..groups1..
                                   ..ctx.. =contains=> {::request-context/org org-id}]

                (with-fake-http [{:url org-url :method :post} (fn [_ {body :body} _]
                                                                (if (= {:type "Organization" :name org-name} (util/from-json body))
                                                                  (let [result {:organization new-org}]
                                                                    {:status 201
                                                                     :body   (util/to-json result)})
                                                                  {:status 422}))]
                  (fact "conveys transformed organizations service response"
                    (let [c (chan)]
                      (orgs/create-organization ..ctx.. config/ORGS_SERVER {:type "Organization" :name org-name} c)
                      (select-keys (<?? c) (keys expected)) => expected)))))))

        (facts "PUT /o/:id"
          (let [org-id      (get rails-org-1 "id")
                org-url     (util/join-path [config/ORGS_SERVER "organizations" org-id])
                new-name    "AWESOME NEW NAME"
                updated-org {:id       (get rails-org-1 "id")
                             :type     "Organization"
                             :name     new-name
                             :uuid     (get rails-org-1 "uuid")
                             :is_admin true
                             :links    {:self                     ..self1..
                                     :projects                 ..projects1..
                                     :organization-memberships ..members1..
                                     :organization-groups      ..groups1..}}]
            (facts "with success"
              (against-background [(routes/self-route ..ctx.. "organization" 1 1) => ..self1..
                                   (routes/org-projects-route ..rt.. org-id) => ..projects1..
                                   (routes/org-memberships-route ..rt.. 1) => ..members1..
                                   (routes/org-groups-route ..rt.. 1) => ..groups1..
                                   ..ctx.. =contains=> {::request-context/org org-id}]

                (with-fake-http [{:url org-url :method :put} (fn [_ {body :body} _]
                                                               (if (= {:organization {:id org-id :name new-name}} (util/from-json body))
                                                                 (let [result-org (assoc rails-org-1 "name" new-name)
                                                                       result     {:organization result-org}]
                                                                   {:status 200
                                                                    :body   (util/to-json result)})
                                                                 {:status 422}))]
                  (fact "conveys transformed organizations service response"
                    (let [c (chan)]
                      (orgs/update-organization ..ctx.. config/ORGS_SERVER c updated-org)
                      (<?? c) => updated-org))

                  (fact "proxies organizations service response"
                    (orgs/update-organization* ..ctx.. config/ORGS_SERVER {:organization updated-org}) => {:organization updated-org}))))))

        (facts "GET /o/:id"
          (let [org-id  (get rails-org-1 "id")
                org-url (util/join-path [config/ORGS_SERVER "organizations" org-id])]
            (facts "with success"
              (let [expected-org {:id       (get rails-org-1 "id")
                                  :type     "Organization"
                                  :name     (get rails-org-1 "name")
                                  :uuid     (get rails-org-1 "uuid")
                                  :is_admin true
                                  :links    {:self                     ..self1..
                                             :projects                 ..projects1..
                                             :organization-memberships ..members1..
                                             :organization-groups      ..groups1..}}]

                (against-background [(routes/self-route ..ctx.. "organization" 1 1) => ..self1..
                                     (routes/org-projects-route ..rt.. org-id) => ..projects1..
                                     (routes/org-memberships-route ..rt.. 1) => ..members1..
                                     (routes/org-groups-route ..rt.. 1) => ..groups1..]
                  (with-fake-http [{:url org-url :method :get} {:status 200
                                                                :body   (util/to-json {:organization rails-org-1})}]
                    (fact "conveys transformed organizations service response"
                      (let [c (chan)]
                        (orgs/get-organization ..ctx.. config/ORGS_SERVER c org-id)
                        (<?? c)) => expected-org)

                    (fact "proxies organizations service response"
                      (orgs/get-organization* ..ctx.. config/ORGS_SERVER) => {:organization expected-org}
                      (provided
                        ..ctx.. =contains=> {::request-context/org org-id}))))))

            (fact "with failure"
              (with-fake-http [{:url org-url :method :get} {:status 401}]
                (fact "conveys throwable"
                  (try+
                    (let [c (chan)]
                      (orgs/get-organization ..ctx.. config/ORGS_SERVER c org-id)
                      (<?? c))
                    (catch [:type :ring.util.http-response/response] _
                      true)) => true)))
            ))

        (facts "GET /o"
          (fact "200 response"
            (let [orgs          [rails-org-1 rails-org-2]
                  expected-orgs [{:id       (get rails-org-1 "id")
                                  :type     "Organization"
                                  :name     (get rails-org-1 "name")
                                  :uuid     (get rails-org-1 "uuid")
                                  :is_admin true
                                  :links    {:self                  ..self1..
                                          :projects                 ..projects1..
                                          :organization-memberships ..members1..
                                          :organization-groups      ..groups1..}}
                                 {:id       (get rails-org-2 "id")
                                  :type     "Organization"
                                  :name     (get rails-org-2 "name")
                                  :uuid     (get rails-org-2 "uuid")
                                  :is_admin true
                                  :links    {:self                  ..self2..
                                          :projects                 ..projects2..
                                          :organization-memberships ..members2..
                                          :organization-groups      ..groups2..}}]]

              (against-background [(routes/self-route ..ctx.. "organization" 1 1) => ..self1..
                                   (routes/self-route ..ctx.. "organization" 2 2) => ..self2..
                                   (routes/org-projects-route ..rt.. (get rails-org-1 "id")) => ..projects1..
                                   (routes/org-projects-route ..rt.. (get rails-org-2 "id")) => ..projects2..
                                   (routes/org-memberships-route ..rt.. 1) => ..members1..
                                   (routes/org-memberships-route ..rt.. 2) => ..members2..
                                   (routes/org-groups-route ..rt.. 1) => ..groups1..
                                   (routes/org-groups-route ..rt.. 2) => ..groups2..]

                (with-fake-http [{:url orgs-url :method :get} {:status 200
                                                               :body   (util/to-json {:organizations orgs})}]

                  (fact "conveys transformed organizations service response"
                    (let [c (chan)]
                      (orgs/get-organizations ..ctx.. config/ORGS_SERVER c)
                      (<?? c)) => expected-orgs)

                  (fact "proxies organizations service response"
                    (orgs/get-organizations* ..ctx.. config/ORGS_SERVER) => {:organizations expected-orgs})))))

          (fact "with failure"
            (with-fake-http [{:url orgs-url :method :get} {:status 401}]
              (fact "conveys throwable"
                (try+
                  (let [c (chan)]
                    (orgs/get-organizations ..ctx.. config/ORGS_SERVER c)
                    (<?? c))
                  (catch [:type :ring.util.http-response/response] _
                    true)) => true)))
          )))))

