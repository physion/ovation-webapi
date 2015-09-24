(ns ovation.test.route-helpers
  (:use midje.sweet)
  (:require [ovation.route-helpers :as r]
            [ovation.routes :as routes]
            [ovation.core :as core]
            [ovation.revisions :as revisions])
  (:import (clojure.lang ExceptionInfo)))

(facts "About get-head-revisions*"
  (fact "returns HEAD revisions for file"
    (r/get-head-revisions* ..req.. ..id..) => ..revs..
    (provided
      ..req.. =contains=> {:auth/auth-info ..auth..}
      (routes/router ..req..) => ..rt..
      (core/get-entities ..auth.. [..id..] ..rt..) => [..file..]
      (revisions/get-head-revisions ..auth.. ..rt.. ..file..) => ..revs..))

  (fact "+throws not-found if file is not found"
    (r/get-head-revisions* ..req.. ..id..) => (throws ExceptionInfo)
    (provided
      ..req.. =contains=> {:auth/auth-info ..auth..}
      (routes/router ..req..) => ..rt..
      (core/get-entities ..auth.. [..id..] ..rt..) => [])))
