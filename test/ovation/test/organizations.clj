(ns ovation.test.organizations
  (:use midje.sweet)
  (:require [ovation.auth :as auth]
            [ovation.organizations :as orgs]
            [ovation.routes :as routes]
            [org.httpkit.fake :refer [with-fake-http]]
            [ovation.util :as util :refer [<??]]
            [ovation.config :as config]
            [ovation.request-context :as request-context]))

(facts "About organizations API"

  (against-background [(request-context/token ..ctx..) => ..auth..
                       ..ctx.. =contains=> {::request-context/auth   ..auth..
                                            ::request-context/routes ..rt..}
                       (request-context/router ..request..) => ..rt..]

    (facts "GET /organizations"
      (let [orgs-url (util/join-path [config/ORGS_SERVER "organizations"])]

        (fact "200 response"
          (with-fake-http [{:url orgs-url :method :get} {:status 200
                                                         :body   (util/to-json {:organizations [..org1.. ..org2..]})}]
            (fact "proxies organizations service response"
              @(orgs/get-organizations ..ctx..) => {:organizations [..org1.. ..org2..]}
              (provided
                (orgs/read-tf ..org1..) => ..org1..
                (orgs/read-tf ..org2..) => ..org2..))))))))
