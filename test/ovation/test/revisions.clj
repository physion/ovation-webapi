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
            [ovation.util :as util]
            [ovation.transform.read :as tr])
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
            rev          {:_id        ..id..
                          :attributes {:created-at ..now..}}
            updated-file {:revisions {..id.. {:status ..status.. :started-at ..now..}}}]
        (rev/update-file-status file [rev] ..status..) => updated-file))
    (fact "sets status COMPLETE if revision is remote [#137147211]"
      (let [file {}
            rev  {:_id        ..id..
                  :attributes {:created-at ..now..
                               :remote     true}}]
        (get-in (rev/update-file-status file [rev] ..status..) [:revisions ..id.. :status]) => k/COMPLETE))
    (fact "handles multiple revisions"
      (let [file         {}
            rev1         {:_id        ..id1..
                          :attributes {:created-at ..now..}}
            rev2         {:_id        ..id2..
                          :attributes {:created-at ..now..}}
            updated-file {:revisions {..id1.. {:status ..status.. :started-at ..now..}
                                      ..id2.. {:status ..status.. :started-at ..now..}}}]
        (rev/update-file-status file [rev1 rev2] ..status..) => updated-file)))

  (against-background [(auth/authenticated-user-id ..auth..) => ..userid..]
    (facts "create-revisions"
      (facts "from a Revision"
        (fact "creates a new revision with resource attribute and appending parent id to previous chain"
          (let [new-revision {:type       k/REVISION-TYPE
                              :attributes {}}
                rev          {:_id  ..revid..
                              :_rev ..revrev..}
                file         {:_id          ..rsrcid..
                              :type         k/FILE-TYPE}]
            (rev/create-revisions ..ctx.. ..db.. ..parent.. [new-revision]) => {:revisions [rev]
                                                                                :links     ..links..
                                                                                :updates   []}
            (provided
              ..parent.. =contains=> {:type         k/REVISION-TYPE
                                      :_id          ..previd..
                                      :attributes   {:file_id  ..rsrcid..
                                                     :previous [..oldprev..]}}
              (core/create-entities ..ctx.. ..db.. [{:type       "Revision"
                                                     :attributes {:previous [..oldprev.. ..previd..]
                                                                  :file_id  ..rsrcid..}}]) => [rev]
              ..rev.. =contains=> {:_id ..revid..}
              (core/get-entities ..ctx.. ..db.. [..rsrcid..]) => [file]
              (rev/update-file-status file [rev] k/UPLOADING) => ..updated-file..
              (links/add-links ..ctx.. ..db.. [..updated-file..] :revisions [..revid..] :inverse-rel :file) => {:updates []
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
              (rev/create-revisions ..ctx.. ..db.. ..file.. [newrev]) => {:revisions [rev]
                                                                          :links     ..links..
                                                                          :updates   []}
              (provided
                ..file.. =contains=> {:type         k/FILE-TYPE
                                      :_id          ..fileid..
                                      :attributes   {}}
                (rev/get-head-revisions ..ctx.. ..db.. ..file..) => [{:type       k/REVISION-TYPE
                                                                      :_id        ..headid..
                                                                      :attributes {:previous []
                                                                                   :file_id  [..fileid..]}}]
                (core/create-entities ..ctx.. ..db.. [{:type       "Revision"
                                                       :attributes {:previous [..headid..]
                                                                    :file_id  ..fileid..}}]) => [rev]
                (links/add-links ..ctx.. ..db.. [..updated-file..] :revisions [..revid..] :inverse-rel :file) => {:updates []
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
              (rev/create-revisions ..ctx.. ..db.. ..file.. [newrev]) => {:revisions [rev]
                                                                          :links     ..links..
                                                                          :updates   []}
              (provided
                ..file.. =contains=> {:type         k/FILE-TYPE
                                      :_id          ..fileid..
                                      :attributes   {}}
                (rev/get-head-revisions ..ctx.. ..db.. ..file..) => []
                (core/create-entities ..ctx.. ..db.. [{:type       "Revision"
                                                       :attributes {:previous [] ;; <- this is key
                                                                    :file_id  ..fileid..}}]) => [rev]
                (rev/update-file-status ..file.. [rev] k/UPLOADING) => ..updated-file..
                (links/add-links ..ctx.. ..db.... [..updated-file..] :revisions [..revid..] :inverse-rel :file) => {:updates []
                                                                                                                    :links   ..links..}))))))

    (facts "get-head-revisions"
      (fact "gets HEAD revisions from couch view"
        (rev/get-head-revisions ..ctx.. ..db.. ..fileid..) => [..rev..]
        (provided
          (couch/get-view ..ctx.. ..db.. k/REVISIONS-VIEW {:startkey      [..org.. ..fileid.. {}]
                                                            :endkey       [..org.. ..fileid..]
                                                            :descending   true
                                                            :include_docs true
                                                            :limit        2} :prefix-teams false) => [..doc..]
          (tr/entities-from-couch [..doc..] ..ctx..) => [..rev..]
          (core/filter-trashed [..rev..] false) => [..rev..]
          ..doc.. =contains=> {:attributes {:previous []}}))

      (fact "returns all HEAD revisions when top 2 are equal"
        (let [doc1 {:attributes {:previous [1 2]}}
              doc2 {:attributes {:previous [1 2]}}]
          (rev/get-head-revisions ..ctx.. ..db.. ..fileid..) => [..rev1.. ..rev2.. ..rev3..]
          (provided
            (couch/get-view ..ctx.. ..db.. k/REVISIONS-VIEW {:startkey     [..org.. ..fileid.. {}]
                                                             :endkey       [..org.. ..fileid..]
                                                             :descending   true
                                                             :include_docs true
                                                             :limit        2} :prefix-teams false) => [doc1 doc2]
            (couch/get-view ..ctx.. ..db.. k/REVISIONS-VIEW {:startkey      [..org.. ..fileid.. 2]
                                                             :endkey        [..org.. ..fileid.. 2]
                                                             :inclusive_end true
                                                             :include_docs  true} :prefix-teams false) => [..doc1.. ..doc2.. ..doc3..]

            (tr/entities-from-couch [..doc1.. ..doc2.. ..doc3..] ..ctx..) => [..rev1.. ..rev2.. ..rev3..]
            (core/filter-trashed [..rev1.. ..rev2.. ..rev3..] false) => [..rev1.. ..rev2.. ..rev3..]))))

    (facts "update-metadata"
      (against-background [(ovation.request-context/token ..ctx..) => "TOKEN"]

        (fact "returns 422 if URL is not ovation.io"
          (let [revision {:_id ..id.. :attributes {:url "https://example.com/rsrc/1"}}]
            (rev/update-metadata ..ctx.. ..db.. revision) => (throws ExceptionInfo)))

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
              (rev/update-metadata ..ctx.. ..db.. revision) => {:_id ..id.. :result true}
              (provided
                (rev/update-file-status file [revision] k/COMPLETE) => ..updated-file..
                (core/get-entity ..ctx.. ..db.. ..fileid..) => file
                (core/update-entities ..ctx.. ..db.. [updated-rev ..updated-file..] :allow-keys [:revisions]) => [{:_id ..id.. :result true} {:_id ..fileid..}]))))

        (fact "skips metadata from Rails and sets file=>revision status to COMPLETE if rails response is not 200 [#136329619]"
          (let [revid       "100"
                revision    {:_id ..id.. :attributes {:url     (util/join-path [config/RESOURCES_SERVER revid])
                                                      :file_id ..fileid..}}
                file        {:_id ..fileid..}
                length      100
                etag        "etag"
                updated-rev (-> revision
                              (assoc-in [:attributes :upload-status] k/COMPLETE))]
            (with-fake-http [{:url (util/join-path [config/RESOURCES_SERVER revid "metadata"]) :method :get} (fn [orig-fn opts callback]
                                                                                                               {:status 500
                                                                                                                :body   (util/to-json {:content_length length
                                                                                                                                       :etag           etag})})]
              (rev/update-metadata ..ctx.. ..db.. revision) => {:_id ..id.. :result true}
              (provided
                (rev/update-file-status file [revision] k/COMPLETE) => ..updated-file..
                (core/get-entity ..ctx.. ..db.. ..fileid..) => file
                (core/update-entities ..ctx.. ..db.. [updated-rev ..updated-file..] :allow-keys [:revisions]) => [{:_id ..id.. :result true} {:_id ..fileid..}]))))))


    (facts "record-upload-failure"
      (fact "updates file=>revision and Revision status to FAILED"
        (let [rev              {:_id        ..revid..
                                :attributes {:file_id    ..fileid..
                                             :created-at ..now..}}
              file             {:_id ..fileid..}

              updated-revision (assoc-in rev [:attributes :upload-status] k/ERROR)
              updated-file     (assoc-in file [:revisions ..revid..] {:status k/ERROR :started-at ..now..})]
          (rev/record-upload-failure ..ctx.. ..db.. rev) => {:revision updated-revision
                                                             :file     updated-file}
          (provided
            (core/get-entity ..ctx.. ..db.. ..fileid..) => file
            (core/update-entities ..ctx.. ..db.. [updated-revision updated-file] :allow-keys [:revisions]) => [updated-revision updated-file]))))


    (facts "make-resource"
      (against-background [(ovation.request-context/token ..ctx..) => "TOKEN"]
        (fact "creates a Rails Resource"
          (let [revid "revid"]
            (with-fake-http [config/RESOURCES_SERVER {:status 201
                                                      :body   (util/to-json {:resource {:public_url "url"
                                                                                        :aws        "aws"
                                                                                        :url        "post"}})}]
              (rev/make-resource ..ctx.. {:_id        revid
                                          :attributes {}}) => {:revision {:_id        revid
                                                                          :attributes {:url           "url"
                                                                                       :upload-status k/UPLOADING}}
                                                               :aws      "aws"
                                                               :post-url "post"}
              (provided
                ..rsrc.. =contains=> {:url ..url..}))))
        (fact "+throws if rails API fails"
          (let [revid "revid"]
            (with-fake-http [config/RESOURCES_SERVER {:status 500
                                                      :body   "{}"}]
              (rev/make-resource ..ctx.. {:_id        revid
                                          :attributes {}}) => (throws ExceptionInfo))))
        (fact "sets [:attributes :remote] to true if :url is present already"
          (let [rev {:attributes {:url ..url..}}]
            (rev/make-resource ..ctx.. rev) => {:revision (-> rev
                                                            (assoc-in [:attributes :remote] true)
                                                            (assoc-in [:attributes :upload-status] k/COMPLETE))
                                                :aws      {}
                                                :post-url ..url..})))
      (fact "does not create a Rails resource if :url is present already"
        (let [rev {:attributes {:url ..url..}}]
          (rev/make-resource ..ctx.. rev) => {:revision (-> rev
                                                          (assoc-in [:attributes :remote] true)
                                                          (assoc-in [:attributes :upload-status] k/COMPLETE))
                                              :aws      {}
                                              :post-url ..url..})))))
