(ns ovation.test.organizations
  (:use midje.sweet)
  (:require [ovation.organizations :as orgs]
            [clojure.core.async :refer [chan]]
            [ovation.util :as util :refer [<??]]
            [org.httpkit.fake :refer [with-fake-http]]
            [ovation.config :as config]
            [ovation.request-context :as request-context]
            [ovation.routes :as routes]))

(facts "About organizations API"
  (let [rails-org-1 {
                     "id"                  1,
                     "uuid"                "1db71db5-9399-4ccb-b7b2-592ae25a810f",
                     "name"                "Name 1",
                     "owner_name"          "Barry Wark",
                     "links"               {
                                            "tags"                        "/api/v1/organizations/201/links/tags",
                                            "folders"                     "/api/v1/organizations/201/links/folders",
                                            "roles"                       "/api/v1/roles?organization_id=201",
                                            "menus"                       "/api/v1/organizations/201/links/menus",
                                            "memberships"                 "/api/v1/organizations/201/links/memberships",
                                            "organization_memberships"    "/api/v1/organizations/201/links/organization_memberships",
                                            "users"                       "/api/v1/wb/users?organization_id=201",
                                            "projects"                    "/api/v1/projects?organization_id=201",
                                            "documents"                   "/api/v1/documents?organization_id=201",
                                            "documents_needing_signature" "/api/v1/documents?needs_signature=true\u0026organization_id=201",
                                            "training_packs"              "/api/v1/organizations/201/links/training_packs",
                                            "requisitions"                "/api/v1/organizations/201/links/requisitions",
                                            "incomplete_requisitions"     "/api/v1/organizations/201/links/requisitions?incomplete=true\u0026per_page=100\u0026sparse=true",
                                            "requisition_templates"       "/api/v1/organizations/201/links/requisition_templates",
                                            "plating_configurations"      "/api/v1/organizations/201/links/plating_configurations",
                                            "insurance_providers"         "/api/v1/organizations/201/links/insurance_providers",
                                            "physicians"                  "/api/v1/organizations/201/links/physicians",
                                            "test_panels"                 "/api/v1/organizations/201/links/test_panels",
                                            "workflows"                   "/api/v1/workflows?organization_id=201",
                                            "workflow_definitions"        "/api/v1/workflow_definitions?organization_id=201",
                                            "barcode_templates"           "/api/v1/barcode_templates?organization_id=201",
                                            "report_configurations"       "/api/v1/report_configurations?organization_id=201"}
                                           ,
                     "is_admin"            true,
                     "training_report_url" "https//services-staging.ovation.io/api/v1/organizations/201/links/training_packs.csv",
                     "custom_attributes"   {},
                     "team"                {
                                            "id"              4264,
                                            "name"            "AccessDx Testing",
                                            "uuid"            "ee4d971c-e3c0-4751-aeef-f44a1a25c3c8",
                                            "organization_id" 201,
                                            "project_id"      nil,
                                            "role_ids"        [
                                                               665,
                                                               738,
                                                               739]}}



        rails-org-2 {
                     "id"                  2,
                     "uuid"                "1db71db5-9399-4ccb-b7b2-592ae25a810f",
                     "name"                "Name 2",
                     "owner_name"          "Barry Wark",
                     "links"               {
                                            "tags"                        "/api/v1/organizations/201/links/tags",
                                            "folders"                     "/api/v1/organizations/201/links/folders",
                                            "roles"                       "/api/v1/roles?organization_id=201",
                                            "menus"                       "/api/v1/organizations/201/links/menus",
                                            "memberships"                 "/api/v1/organizations/201/links/memberships",
                                            "organization_memberships"    "/api/v1/organizations/201/links/organization_memberships",
                                            "users"                       "/api/v1/wb/users?organization_id=201",
                                            "projects"                    "/api/v1/projects?organization_id=201",
                                            "documents"                   "/api/v1/documents?organization_id=201",
                                            "documents_needing_signature" "/api/v1/documents?needs_signature=true\u0026organization_id=201",
                                            "training_packs"              "/api/v1/organizations/201/links/training_packs",
                                            "requisitions"                "/api/v1/organizations/201/links/requisitions",
                                            "incomplete_requisitions"     "/api/v1/organizations/201/links/requisitions?incomplete=true\u0026per_page=100\u0026sparse=true",
                                            "requisition_templates"       "/api/v1/organizations/201/links/requisition_templates",
                                            "plating_configurations"      "/api/v1/organizations/201/links/plating_configurations",
                                            "insurance_providers"         "/api/v1/organizations/201/links/insurance_providers",
                                            "physicians"                  "/api/v1/organizations/201/links/physicians",
                                            "test_panels"                 "/api/v1/organizations/201/links/test_panels",
                                            "workflows"                   "/api/v1/workflows?organization_id=201",
                                            "workflow_definitions"        "/api/v1/workflow_definitions?organization_id=201",
                                            "barcode_templates"           "/api/v1/barcode_templates?organization_id=201",
                                            "report_configurations"       "/api/v1/report_configurations?organization_id=201"}
                                           ,
                     "is_admin"            true,
                     "training_report_url" "https//services-staging.ovation.io/api/v1/organizations/201/links/training_packs.csv",
                     "custom_attributes"   {},
                     "team"                {
                                            "id"              4264,
                                            "name"            "AccessDx Testing",
                                            "uuid"            "ee4d971c-e3c0-4751-aeef-f44a1a25c3c8",
                                            "organization_id" 201,
                                            "project_id"      nil,
                                            "role_ids"        [
                                                               665,
                                                               738,
                                                               739]}}]



    (against-background [(request-context/token ..ctx..) => ..auth..
                         ..ctx.. =contains=> {::request-context/auth   ..auth..
                                              ::request-context/routes ..rt..}
                         (request-context/router ..request..) => ..rt..]

      (let [orgs-url (util/join-path [config/ORGS_SERVER "organizations"])]

        (facts "GET /o/:id"
          (let [org-id       (get rails-org-1 "id")
                org-url      (util/join-path [config/ORGS_SERVER "organizations" org-id])]
            (fact "200 response"
              (let [expected-org {:id    (get rails-org-1 "id")
                                  :type  "Organization"
                                  :name  (get rails-org-1 "name")
                                  :uuid  (get rails-org-1 "uuid")
                                  :links {:self                     ..self1..
                                          :projects                 ..projects1..
                                          :organization-memberships ..members1..
                                          :organization-groups      ..groups1..}}]

                (against-background [(routes/self-route ..ctx.. "organization" 1) => ..self1..
                                     (routes/org-projects-route ..rt.. org-id) => ..projects1..
                                     (routes/org-memberships-route ..rt.. 1) => ..members1..
                                     (routes/org-groups-route ..rt.. 1) => ..groups1..]
                  (with-fake-http [{:url org-url :method :get} {:status 200
                                                                :body   (util/to-json {:organization rails-org-1})}]
                    (fact "conveys transformed organizations service response"
                      (let [c (chan)]
                        (orgs/get-organization ..ctx.. c org-id)
                        (<?? c)) => expected-org)))))
            (fact "with failure"
              (with-fake-http [{:url org-url :method :get} {:status 401}]

                (fact "conveys throwable"
                  (let [c (chan)]
                    (orgs/get-organization ..ctx.. c 1)
                    (<?? c)) =throws=> anything)))))

        (facts "GET /o"
          (fact "200 response"
            (let [orgs          [rails-org-1 rails-org-2]
                  expected-orgs [{:id    (get rails-org-1 "id")
                                  :type  "Organization"
                                  :name  (get rails-org-1 "name")
                                  :uuid  (get rails-org-1 "uuid")
                                  :links {:self                     ..self1..
                                          :projects                 ..projects1..
                                          :organization-memberships ..members1..
                                          :organization-groups      ..groups1..}}
                                 {:id    (get rails-org-2 "id")
                                  :type  "Organization"
                                  :name  (get rails-org-2 "name")
                                  :uuid  (get rails-org-2 "uuid")
                                  :links {:self                     ..self2..
                                          :projects                 ..projects2..
                                          :organization-memberships ..members2..
                                          :organization-groups      ..groups2..}}]]

              (against-background [(routes/self-route ..ctx.. "organization" 1) => ..self1..
                                   (routes/self-route ..ctx.. "organization" 2) => ..self2..
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
                      (orgs/get-organizations ..ctx.. c)
                      (<?? c)) => expected-orgs)

                  (fact "proxies organizations service response"
                    (orgs/get-organizations* ..ctx..) => {:organizations expected-orgs})))))

          (fact "with failure"
            (with-fake-http [{:url orgs-url :method :get} {:status 401}]

              (fact "conveys throwable"
                (let [c (chan)]
                  (orgs/get-organizations ..ctx.. c)
                  (<?? c)) =throws=> anything))))))))

