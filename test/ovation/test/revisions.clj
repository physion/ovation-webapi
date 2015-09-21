(ns ovation.test.revisions
  (:use midje.sweet)
  (:require [ovation.revisions :as rev]
            [slingshot.slingshot :refer [throw+]]
            [ovation.core :as core]
            [ovation.links :as links]
            [ovation.auth :as auth]
            [ovation.constants :as k]
            [ovation.couch :as couch]))


(facts "About Revisions"
  (against-background [(auth/authenticated-user-id ..auth..) => ..userid..]
    (facts "creation"
      (facts "`create-revisions`"
        (facts "from a Revision"
          (fact "creates a new revision with resource attribute and appending parent id to previous chain"
            (let [new-revision {:type       k/REVISION-TYPE
                                :attributes {}}
                  rev {:_id ..revid..}
                  file {:_id  ..rsrcid..
                        :type k/FILE-TYPE}]
              (rev/create-revisions ..auth.. ..rt.. ..parent.. [new-revision]) => {:revisions [rev]
                                                                                   :links     ..links..
                                                                                   :updates   []}
              (provided
                ..parent.. =contains=> {:type       k/REVISION-TYPE
                                        :_id        ..previd..
                                        :attributes {:file_id ..rsrcid..
                                                     :previous [..oldprev..]}}
                (core/create-entities ..auth.. [{:type       "Revision"
                                                 :attributes {:previous [..oldprev.. ..previd..]
                                                              :file_id ..rsrcid..}}] ..rt..) => [rev]
                ..rev.. =contains=> {:_id ..revid..}
                (core/get-entities ..auth.. [..rsrcid..] ..rt..) => [file]
                (links/add-links ..auth.. [file] :revisions [..revid..] ..rt.. :inverse-rel :file) => {:updates []
                                                                                                           :links   ..links..}))))
        (facts "from a File"
          (facts "with single HEAD revision"
            (fact "creates a new revision"
              (let [newrev {:type       k/REVISION-TYPE
                            :attributes {}}
                    rev {:type       k/REVISION-TYPE
                         :_id        ..revid..
                         :attributes {:previous [..headid..]
                                      :file_id ..fileid..}}]
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
                                                                                      :file_id [..fileid..]}}]
                  (core/create-entities ..auth.. [{:type       "Revision"
                                                   :attributes {:previous [..headid..]
                                                                :file_id ..fileid..}}] ..rt..) => [rev]
                  (links/add-links ..auth.. [..file..] :revisions [..revid..] ..rt.. :inverse-rel :file) => {:updates []
                                                                                                                 :links   ..links..}))))
          (facts "without HEAD revision"
            (fact "creates a new revision"
              (let [newrev {:type       k/REVISION-TYPE
                            :attributes {}}
                    rev {:type       k/REVISION-TYPE
                         :_id        ..revid..
                         :attributes {:previous [..headid..]
                                      :file_id ..fileid..}}]
                (rev/create-revisions ..auth.. ..rt.. ..file.. [newrev]) => {:revisions [rev]
                                                                             :links     ..links..
                                                                             :updates   []}
                (provided
                  ..file.. =contains=> {:type       k/FILE-TYPE
                                        :_id        ..fileid..
                                        :attributes {}}
                  (rev/get-head-revisions ..auth.. ..rt.. ..file..) => []
                  (core/create-entities ..auth.. [{:type       "Revision"
                                                   :attributes {:previous []
                                                                :file_id ..fileid..}}] ..rt..) => [rev]
                  (links/add-links ..auth.. [..file..] :revisions [..revid..] ..rt.. :inverse-rel :file) => {:updates []
                                                                                                                 :links   ..links..})))))))

    (facts "HEAD"
      (facts "`get-head-revisions`"
        (fact "gets HEAD revisions from couch view"
          (rev/get-head-revisions ..auth.. ..rt.. ..file..) => [..rev..]
          (provided
            (couch/db ..auth..) => ..db..
            ..file.. =contains=> {:_id ..fileid..}
            (couch/get-view ..db.. k/REVISIONS-VIEW {:reduce   true
                                                     :group    true
                                                     :startkey ..fileid..
                                                     :endkey   ..fileid..}) => {:rows [{:key   ..fileid..
                                                                                        :value [[..revid..], 3]}]}
            (core/get-entities ..auth.. [..revid..] ..rt..) => [..rev..]))))))
