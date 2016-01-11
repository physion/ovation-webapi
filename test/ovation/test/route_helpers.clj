(ns ovation.test.route-helpers
  (:use midje.sweet)
  (:require [ovation.route-helpers :as r]
            [ovation.routes :as routes]
            [ovation.core :as core]
            [ovation.revisions :as revisions]
            [ovation.auth :as auth])
  (:import (clojure.lang ExceptionInfo)))

(facts "About get-head-revisions*"
  (fact "returns HEAD revisions for file"
    (r/get-head-revisions* ..req.. ..id..) => {:body {:revisions ..revs..} :headers {} :status 200}
    (provided
      ..req.. =contains=> {::auth/auth-info ..auth..}
      ..file.. =contains=> {:type "File"}
      (routes/router ..req..) => ..rt..
      (core/get-entities ..auth.. [..id..] ..rt..) => (seq [..file..])
      (revisions/get-head-revisions ..auth.. ..rt.. ..file..) => ..revs..))

  (fact "+throws not-found if file is not found"
    (r/get-head-revisions* ..req.. ..id..) => (throws ExceptionInfo)
    (provided
      ..req.. =contains=> {::auth/auth-info ..auth..}
      (routes/router ..req..) => ..rt..
      (core/get-entities ..auth.. [..id..] ..rt..) => [])))

(facts "About post-resource*"
  (fact "adds relationships for a new Source"
    (let [source-entity {:type "Source"
                         :_id  ..id2..}
          file-entity {:type "File"
                       :_id  ..fileid..}]
      (:body (r/post-resource* ..req.. "file" ..fileid.. [{:type       "Source"
                                                           :attributes {}}])) => {:entities (seq [source-entity])
                                                                                  :links    ..links..
                                                                                  :updates  ..updates..}
      (provided
        ..req.. =contains=> {::auth/auth-info ..auth..}

        (routes/router ..req..) => ..rt..
        (core/get-entities ..auth.. [..fileid..] ..rt..) => (seq [file-entity])
        (core/get-entities ..auth.. [..id2..] ..rt..) => (seq [source-entity])
        (core/create-entities ..auth.. [{:type "Source" :attributes {}}] ..rt.. :parent ..fileid..) => [source-entity]
        (core/create-values ..auth.. ..rt.. anything) => ..links..
        (core/update-entities ..auth.. anything ..rt..) => ..updates..))))
