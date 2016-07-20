(ns ovation.test.route-helpers
  (:use midje.sweet)
  (:require [ovation.route-helpers :as r]
            [ovation.routes :as routes]
            [ovation.core :as core]
            [ovation.revisions :as revisions]
            [ovation.auth :as auth]
            [ovation.links :as links]
            [ovation.constants :as k]
            [ovation.util :as util])
  (:import (clojure.lang ExceptionInfo)))

(facts "About get-head-revisions*"
  (fact "returns HEAD revisions for file"
    (r/get-head-revisions* ..req.. ..id..) => {:body {:revisions ..revs..} :headers {} :status 200}
    (provided
      ..req.. =contains=> {:identity ..auth..}
      ..file.. =contains=> {:type "File"}
      (routes/router ..req..) => ..rt..
      (core/get-entities ..auth.. [..id..] ..rt..) => (seq [..file..])
      (revisions/get-head-revisions ..auth.. ..rt.. ..file..) => ..revs..))

  (fact "+throws not-found if file is not found"
    (r/get-head-revisions* ..req.. ..id..) => (throws ExceptionInfo)
    (provided
      ..req.. =contains=> {:identity ..auth..}
      (routes/router ..req..) => ..rt..
      (core/get-entities ..auth.. [..id..] ..rt..) => [])))

(facts "About post-resource*"
  (fact "adds embedded relationships"
    (let [project      {:type k/PROJECT-TYPE
                        :_id  ..projectid..}
          rev          {:type k/REVISION-TYPE
                        :_id  ..revid..}
          new-activity {:type          k/ACTIVITY-TYPE
                        :relationships {:inputs {:related     [(:_id rev)]
                                                 :type        k/REVISION-TYPE
                                                 :inverse_rel :activities}}}
          activity     (assoc new-activity :_id (util/make-uuid))]
      (:body (r/post-resource* ..req.. k/PROJECT-TYPE ..projectid.. [new-activity])) => {:entities [activity]
                                                                                         :links    ..links..
                                                                                         :updates  ..updates..}
      (provided
        ..req.. =contains=> {:identity ..auth..}
        (routes/router ..req..) => ..rt..
        (r/remove-embedded-relationships [new-activity]) => [(dissoc new-activity :relationships)]
        (core/get-entities ..auth.. [..projectid..] ..rt..) => [project]
        (core/create-entities ..auth.. [(dissoc new-activity :relationships)] ..rt.. :parent ..projectid..) => [activity]
        (links/add-links ..auth.. [activity] :inputs [(:_id rev)] ..rt.. :inverse-rel :activities) => {:updates ..updates..
                                                                                                       :links   ..embedded..}
        (links/add-links ..auth.. [project] "activities" [(:_id activity)] ..rt.. :inverse-rel "parents") => {:updates ..updates..
                                                                                                              :links   ..links..}
        (core/create-values ..auth.. ..rt.. anything) => ..links..
        (core/update-entities ..auth.. anything ..rt.. :authorize false :update-collaboration-roots true) => ..updates..)))


  (fact "handles embedded relationships with :create_as_inverse == true"
    (let [project      {:type k/PROJECT-TYPE
                        :_id  ..projectid..}
          new-activity {:type          k/ACTIVITY-TYPE
                        :relationships {:parents {:related           [(:_id project)]
                                                  :type              k/PROJECT-TYPE
                                                  :inverse_rel       :activities
                                                  :create_as_inverse true}}}
          activity     (assoc new-activity :_id (util/make-uuid))]
      (:body (r/post-resource* ..req.. k/PROJECT-TYPE ..projectid.. [new-activity])) => {:entities [activity]
                                                                                         :links    ..links..
                                                                                         :updates  ..updates..}
      (provided
        ..req.. =contains=> {:identity ..auth..}
        (routes/router ..req..) => ..rt..
        (core/get-entities ..auth.. [..projectid..] ..rt..) => [project]
        (r/remove-embedded-relationships [new-activity]) => [(dissoc new-activity :relationships)]
        (links/add-links ..auth.. anything :activities [(:_id activity)] ..rt.. :inverse-rel :parents) => {:updates ..updates..
                                                                                                            :links   ..embedded..}
        (links/add-links ..auth.. anything "activities" [(:_id activity)] ..rt.. :inverse-rel "parents") => {:updates ..updates..
                                                                                                              :links   ..links..}

        (core/create-entities ..auth.. [(dissoc new-activity :relationships)] ..rt.. :parent ..projectid..) => [activity]
        (core/create-values ..auth.. ..rt.. anything) => ..links..
        (core/update-entities ..auth.. anything ..rt.. :authorize false :update-collaboration-roots true) => ..updates..)))

  (fact "adds relationships for a new Source"
    (let [source-entity {:type "Source"
                         :_id  ..id2..}
          file-entity   {:type "File"
                         :_id  ..fileid..}]
      (:body (r/post-resource* ..req.. "file" ..fileid.. [{:type       "Source"
                                                           :attributes {}}])) => {:entities (seq [source-entity])
                                                                                  :links    ..links..
                                                                                  :updates  ..updates..}
      (provided
        ..req.. =contains=> {:identity ..auth..}
        (routes/router ..req..) => ..rt..
        (core/get-entities ..auth.. [..fileid..] ..rt..) => (seq [file-entity])
        (core/get-entities ..auth.. [..id2..] ..rt..) => (seq [source-entity])
        (core/create-entities ..auth.. [{:type "Source" :attributes {}}] ..rt.. :parent ..fileid..) => [source-entity]
        (core/create-values ..auth.. ..rt.. anything) => ..links..
        (core/update-entities ..auth.. anything ..rt.. :authorize false :update-collaboration-roots true) => ..updates..))))

(facts "About move-file*"
  (fact "fails if entity not file or folder"
    (let [src  {:type "Folder"
                :_id  ..src..}
          file {:type "Whoa"
                :_id  ..file..}
          dest {:type "Folder"
                :_id  ..dest..}]

      (r/move-contents* ..req.. ..file.. {:source ..src.. :destination ..dest..}) => (throws ExceptionInfo)
      (provided
        ..req.. =contains=> {:identity ..auth..}
        (routes/router ..req..) => ..rt..
        (core/get-entities ..auth.. [..src..] ..rt..) => (seq [src])
        (core/get-entities ..auth.. [..dest..] ..rt..) => (seq [dest])
        (core/get-entities ..auth.. [..file..] ..rt..) => (seq [file]))))

  (fact "fails if src not a folder"
    (let [src  {:type "Whoa"
                :_id  ..src..}
          file {:type "File"
                :_id  ..file..}
          dest {:type "Folder"
                :_id  ..dest..}]

      (r/move-contents* ..req.. ..file.. {:source ..src.. :destination ..dest..}) => (throws ExceptionInfo)
      (provided
        ..req.. =contains=> {:identity ..auth..}
        (routes/router ..req..) => ..rt..
        (core/get-entities ..auth.. [..src..] ..rt..) => (seq [src])
        (core/get-entities ..auth.. [..dest..] ..rt..) => (seq [dest])
        (core/get-entities ..auth.. [..file..] ..rt..) => (seq [file]))))

  (fact "fails if dest not a folder"
    (let [src  {:type "Folder"
                :_id  ..src..}
          file {:type "File"
                :_id  ..file..}
          dest {:type "Whoa"
                :_id  ..dest..}]

      (r/move-contents* ..req.. ..file.. {:source ..src.. :destination ..dest..}) => (throws ExceptionInfo)
      (provided
        ..req.. =contains=> {:identity ..auth..}
        (routes/router ..req..) => ..rt..
        (core/get-entities ..auth.. [..src..] ..rt..) => (seq [src])
        (core/get-entities ..auth.. [..dest..] ..rt..) => (seq [dest])
        (core/get-entities ..auth.. [..file..] ..rt..) => (seq [file]))))

  (fact "adds relationships"
    (let [src  {:type "Folder"
                :_id  ..src..}
          file {:type "File"
                :_id  ..file..}
          dest {:type "Folder"
                :_id  ..dest..}]

      (r/move-contents* ..req.. ..file.. {:source ..src.. :destination ..dest..}) => {:file    file
                                                                                      :links   ..created-links..
                                                                                      :updates ..updated-entities..}
      (provided
        ..req.. =contains=> {:identity ..auth..}
        (routes/router ..req..) => ..rt..
        (core/get-entities ..auth.. [..src..] ..rt..) => (seq [src])
        (core/get-entities ..auth.. [..dest..] ..rt..) => (seq [dest])
        (core/get-entities ..auth.. [..file..] ..rt..) => (seq [file])
        (links/add-links ..auth.. [dest] "files" [..file..] ..rt.. :inverse-rel "parents") => {:links   ..links..
                                                                                               :updates ..updates..}
        (links/delete-links ..auth.. ..rt.. src "files" ..file..) => ..deleted..
        (core/create-values ..auth.. ..rt.. ..links..) => ..created-links...
        (core/update-entities ..auth.. ..updates.. ..rt.. :authorize false :update-collaboration-roots true) => ..updated-entities..))))

