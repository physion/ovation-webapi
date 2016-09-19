(ns ovation.test.revisions
  (:use midje.sweet)
  (:require [ovation.revisions :as rev]
            [slingshot.slingshot :refer [throw+]]
            [ovation.core :as core]
            [ovation.links :as links]
            [ovation.auth :as auth]
            [ovation.constants :as k]
            [ovation.couch :as couch]
            [ovation.config :as config]
            [org.httpkit.fake :refer [with-fake-http]]
            [ovation.util :as util])
  (:import (clojure.lang ExceptionInfo)))

(defn sling-throwable
  [exception-map]
  (slingshot.support/get-throwable (slingshot.support/make-context
                                     exception-map
                                     (str "throw+: " map)
                                     nil
                                     (slingshot.support/stack-trace))))


(facts "About Revisions"
  (facts "update-file-status"
    (fact "adds revision to status"
      (let [file         {}
            rev          {:_id ..id..}
            updated-file {:revisions {..id.. {:status ..status..}}}]
        (rev/update-file-status file [rev] ..status..) => updated-file))
    (fact "handles multiple revisions"
      (let [file         {}
            rev1         {:_id ..id1..}
            rev2         {:_id ..id2..}
            updated-file {:revisions {..id1.. {:status ..status..}
                                      ..id2.. {:status ..status..}}}]
        (rev/update-file-status file [rev1 rev2] ..status..) => updated-file)))

  (against-background [(auth/authenticated-user-id ..auth..) => ..userid..]
    (facts "create-revisions"
      (facts "from a Revision"
        (fact "creates a new revision with resource attribute and appending parent id to previous chain"
          (let [new-revision {:type       k/REVISION-TYPE
                              :attributes {}}
                rev          {:_id  ..revid..
                              :_rev ..revrev..}
                file         {:_id  ..rsrcid..
                              :type k/FILE-TYPE}]
            (rev/create-revisions ..auth.. ..rt.. ..parent.. [new-revision]) => {:revisions [rev]
                                                                                 :links     ..links..
                                                                                 :updates   []}
            (provided
              ..parent.. =contains=> {:type       k/REVISION-TYPE
                                      :_id        ..previd..
                                      :attributes {:file_id  ..rsrcid..
                                                   :previous [..oldprev..]}}
              (core/create-entities ..auth.. [{:type       "Revision"
                                               :attributes {:previous [..oldprev.. ..previd..]
                                                            :file_id  ..rsrcid..}}] ..rt..) => [rev]
              ..rev.. =contains=> {:_id ..revid..}
              (core/get-entities ..auth.. [..rsrcid..] ..rt..) => [file]
              (rev/update-file-status file [rev] k/UPLOADING) => ..updated-file..
              (links/add-links ..auth.. [..updated-file..] :revisions [..revid..] ..rt.. :inverse-rel :file) => {:updates []
                                                                                                                 :links   ..links..}))))
      (facts "from a File"
        (facts "with single HEAD revision"
          (fact "creates a new revision"
            (let [newrev {:type       k/REVISION-TYPE
                          :attributes {}}
                  rev    {:type       k/REVISION-TYPE
                          :_rev       ..revrev..
                          :_id        ..revid..
                          :attributes {:previous [..headid..]
                                       :file_id  ..fileid..}}]
              (rev/create-revisions ..auth.. ..rt.. ..file.. [newrev]) => {:revisions [rev]
                                                                           :links     ..links..
                                                                           :updates   []}
              (provided
                ..file.. =contains=> {:type       k/FILE-TYPE
                                      :_id        ..fileid..
                                      :attributes {}}
                (rev/get-head-revisions ..auth.. ..rt.. ..file..) => [{:type       k/REVISION-TYPE
                                                                       :_id        ..headid..
                                                                       :attributes {:previous []
                                                                                    :file_id  [..fileid..]}}]
                (core/create-entities ..auth.. [{:type       "Revision"
                                                 :attributes {:previous [..headid..]
                                                              :file_id  ..fileid..}}] ..rt..) => [rev]
                (links/add-links ..auth.. [..updated-file..] :revisions [..revid..] ..rt.. :inverse-rel :file) => {:updates []
                                                                                                                   :links   ..links..}
                (rev/update-file-status ..file.. [rev] k/UPLOADING) => ..updated-file..))))

        (facts "without HEAD revision"
          (fact "creates a new revision"
            (let [newrev {:type       k/REVISION-TYPE
                          :attributes {}}
                  rev    {:type       k/REVISION-TYPE
                          :_id        ..revid..
                          :attributes {:previous []
                                       :file_id  ..fileid..}}]
              (rev/create-revisions ..auth.. ..rt.. ..file.. [newrev]) => {:revisions [rev]
                                                                           :links     ..links..
                                                                           :updates   []}
              (provided
                ..file.. =contains=> {:type       k/FILE-TYPE
                                      :_id        ..fileid..
                                      :attributes {}}
                (rev/get-head-revisions ..auth.. ..rt.. ..file..) => []
                (core/create-entities ..auth.. [{:type       "Revision"
                                                 :attributes {:previous [] ;; <- this is key
                                                              :file_id  ..fileid..}}] ..rt..) => [rev]
                (rev/update-file-status ..file.. [rev] k/UPLOADING) => ..updated-file..
                (links/add-links ..auth.. [..updated-file..] :revisions [..revid..] ..rt.. :inverse-rel :file) => {:updates []
                                                                                                                   :links   ..links..}))))))

    (facts "get-head-revisions"
      (fact "gets HEAD revisions from couch view"
        (rev/get-head-revisions ..auth.. ..rt.. ..fileid..) => [..rev..]
        (provided
          (couch/db ..auth..) => ..db..
          (couch/get-view ..auth.. ..db.. k/REVISIONS-VIEW {:reduce   true
                                                            :group    true
                                                            :startkey ..fileid..
                                                            :endkey   ..fileid..}) => [{:key   ..fileid..
                                                                                        :value [[..revid..], 3]}]
          (core/get-entities ..auth.. [..revid..] ..rt..) => [..rev..]))

      (fact "returns all HEAD revisions"
        (rev/get-head-revisions ..auth.. ..rt.. ..fileid..) => [..rev1.. ..rev2..]
        (provided
          (couch/db ..auth..) => ..db..
          (couch/get-view ..auth.. ..db.. k/REVISIONS-VIEW {:reduce   true
                                                            :group    true
                                                            :startkey ..fileid..
                                                            :endkey   ..fileid..}) => [{:key   ..fileid..
                                                                                        :value [[..revid1.. ..revid2..], 3]}]
          (core/get-entities ..auth.. [..revid1.. ..revid2..] ..rt..) => [..rev1.. ..rev2..])))

    (facts "update-metadata"
      (fact "returns 422 if URL is not ovation.io"
        (let [revision {:_id ..id.. :attributes {:url "https://example.com/rsrc/1"}}]
          (rev/update-metadata ..auth.. ..rt.. revision) => (throws ExceptionInfo)))

      (fact "updates metadata from Rails and sets file=>revision status to COMPLETE"
        (let [revid       "100"
              revision    {:_id ..id.. :attributes {:url     (util/join-path [config/RESOURCES_SERVER revid])
                                                    :file_id ..fileid..}}
              file        {:_id ..fileid..}
              length      100
              etag        "etag"
              updated-rev (-> revision
                            (assoc-in [:attributes :content_length] length)
                            (assoc-in [:attributes :upload-status] k/COMPLETE))]
          (with-fake-http [{:url (util/join-path [config/RESOURCES_SERVER revid "metadata"]) :method :get} (fn [orig-fn opts callback]
                                                                                                             {:status 200
                                                                                                              :body   (util/to-json {:content_length length
                                                                                                                                     :etag           etag})})]
            (rev/update-metadata ..auth.. ..rt.. revision) => {:_id ..id.. :result true}
            (provided
              (rev/update-file-status file [revision] k/COMPLETE) => ..updated-file..
              (core/get-entity ..auth.. ..fileid.. ..rt..) => file
              (core/update-entities ..auth.. [updated-rev ..updated-file..] ..rt..) => [{:_id ..id.. :result true} {:_id ..fileid..}])))))


    (facts "record-upload-failure"
      (fact "updates file=>revision and Revision status to FAILED"
        (let [rev              {:_id        ..revid..
                                :attributes {:file_id ..fileid..}}
              file             {:_id ..fileid..}

              updated-revision (assoc-in rev [:attributes :upload-status] k/ERROR)
              updated-file     (assoc-in file [:revisions ..revid..] {:status k/ERROR})]
          (rev/record-upload-failure ..auth.. ..rt.. rev) => {:revision updated-revision
                                                              :file     updated-file}
          (provided
            (core/get-entity ..auth.. ..fileid.. ..rt..) => file
            (core/update-entities ..auth.. [updated-revision updated-file] ..rt..) => [updated-revision updated-file]))))


    (facts "make-resource"
      (fact "creates a Rails Resource"
        (let [revid "revid"]
          (with-fake-http [config/RESOURCES_SERVER {:status 201
                                                    :body   (util/to-json {:resource {:public_url "url"
                                                                                      :aws        "aws"
                                                                                      :url        "post"}})}]
            (rev/make-resource ..auth.. {:_id        revid
                                         :attributes {}}) => {:revision {:_id        revid
                                                                         :attributes {:url "url"}}
                                                              :aws      "aws"
                                                              :post-url "post"}
            (provided
              ..rsrc.. =contains=> {:url ..url..}))))
      (fact "+throws if rails API fails"
        (let [revid "revid"]
          (with-fake-http [config/RESOURCES_SERVER {:status 500
                                                    :body   "{}"}]
            (rev/make-resource ..auth.. {:_id        revid
                                         :attributes {}}) => (throws ExceptionInfo))))
      (fact "does not create a Rails resource if :url is present already"
        (rev/make-resource ..auth.. ..rev..) => {:revision ..rev..
                                                 :aws      {}
                                                 :post-url ..url..}
        (provided
          ..rev.. =contains=> {:attributes {:url ..url..}})))))
