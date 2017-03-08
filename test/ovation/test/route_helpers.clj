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

(defn sling-throwable
  [exception-map]
  (slingshot.support/get-throwable (slingshot.support/make-context
                                     exception-map
                                     (str "throw+: " map)
                                     nil
                                     (slingshot.support/stack-trace))))

(facts "About get-head-revisions*"
  (fact "returns HEAD revisions for file"
    (r/get-head-revisions* ..req.. ..db.. ..id..) => {:body {:revisions ..revs..} :headers {} :status 200}
    (provided
      ..req.. =contains=> {:identity ..auth..}
      ..file.. =contains=> {:type "File"}
      (routes/router ..req..) => ..rt..
      (revisions/get-head-revisions ..auth.. ..db.. ..rt.. ..id..) => ..revs..))

  (fact "+throws not-found if HEADs throws not found"
    (r/get-head-revisions* ..req.. ..db.. ..id..) => (throws ExceptionInfo)
    (provided
      ..req.. =contains=> {:identity ..auth..}
      (routes/router ..req..) => ..rt..
      (revisions/get-head-revisions ..auth.. ..db.. ..rt.. ..id..) =throws=> (sling-throwable {:type ::revisions/not-found}))))

(facts "About post-resource*"
  (fact "adds embedded relationships"
    (let [project      {:type         k/PROJECT-TYPE
                        :_id          ..projectid..
                        :organization ..org..}
          rev          {:type         k/REVISION-TYPE
                        :_id          ..revid..
                        :organization ..org..}
          new-activity {:type          k/ACTIVITY-TYPE
                        :relationships {:inputs {:related     [(:_id rev)]
                                                 :type        k/REVISION-TYPE
                                                 :inverse_rel :activities}}}
          activity     (-> new-activity
                         (assoc :_id (util/make-uuid))
                         (assoc :organization ..org..))]
      (:body (r/post-resource* ..req.. ..db.. ..org.. k/PROJECT-TYPE ..projectid.. [new-activity])) => {:entities [activity]
                                                                                                        :links    ..links..
                                                                                                        :updates  ..updates..}
      (provided
        ..req.. =contains=> {:identity ..auth..}
        (routes/router ..req..) => ..rt..
        (r/remove-embedded-relationships [new-activity]) => [(dissoc new-activity :relationships)]
        (core/get-entities ..auth.. ..db.. ..org.. [..projectid..] ..rt..) => [project]
        (core/create-entities ..auth.. ..db.. ..org.. [(dissoc new-activity :relationships)] ..rt.. :parent ..projectid..) => [activity]
        (links/add-links ..auth.. ..db.. ..org.. [activity] :inputs [(:_id rev)] ..rt.. :inverse-rel :activities) => {:updates ..updates..
                                                                                                                      :links   ..embedded..}
        (links/add-links ..auth.. ..db.. ..org.. [project] "activities" [(:_id activity)] ..rt.. :inverse-rel "parents") => {:updates ..updates..
                                                                                                                             :links   ..links..}
        (core/create-values ..auth.. ..db.. ..rt.. ..org.. anything) => ..links..
        (core/update-entities ..auth.. ..db.. ..org.. anything ..rt.. :authorize false :update-collaboration-roots true) => ..updates..)))


  (fact "handles embedded relationships with :create_as_inverse == true"
    (let [project      {:type         k/PROJECT-TYPE
                        :_id          ..projectid..
                        :organization ..org..}
          new-activity {:type          k/ACTIVITY-TYPE
                        :relationships {:parents {:related           [(:_id project)]
                                                  :type              k/PROJECT-TYPE
                                                  :inverse_rel       :activities
                                                  :create_as_inverse true}}}
          activity     (-> new-activity
                         (assoc :_id (util/make-uuid))
                         (assoc :organization ..org..))]
      (:body (r/post-resource* ..req.. ..db.. ..org.. k/PROJECT-TYPE ..projectid.. [new-activity])) => {:entities [activity]
                                                                                                        :links    ..links..
                                                                                                        :updates  ..updates..}
      (provided
        ..req.. =contains=> {:identity ..auth..}
        (routes/router ..req..) => ..rt..
        (core/get-entities ..auth.. ..db.. ..org.. [..projectid..] ..rt..) => [project]
        (r/remove-embedded-relationships [new-activity]) => [(dissoc new-activity :relationships)]
        (links/add-links ..auth.. ..db.. ..org.. anything :activities [(:_id activity)] ..rt.. :inverse-rel :parents) => {:updates ..updates..
                                                                                                                          :links   ..embedded..}
        (links/add-links ..auth.. ..db.. ..org.. anything "activities" [(:_id activity)] ..rt.. :inverse-rel "parents") => {:updates ..updates..
                                                                                                                            :links   ..links..}

        (core/create-entities ..auth.. ..db.. ..org.. [(dissoc new-activity :relationships)] ..rt.. :parent ..projectid..) => [activity]
        (core/create-values ..auth.. ..db.. ..rt.. ..org.. anything) => ..links..
        (core/update-entities ..auth.. ..db.. ..org.. anything ..rt.. :authorize false :update-collaboration-roots true) => ..updates..)))

  (fact "adds relationships for a new Source"
    (let [source-entity {:type         "Source"
                         :_id          ..id2..
                         :organization ..org..}
          file-entity   {:type         "File"
                         :_id          ..fileid..
                         :organization ..org..}]
      (:body (r/post-resource* ..req.. ..db.. ..org.. "file" ..fileid.. [{:type       "Source"
                                                                          :attributes {}}])) => {:entities (seq [source-entity])
                                                                                                 :links    ..links..
                                                                                                 :updates  ..updates..}
      (provided
        ..req.. =contains=> {:identity ..auth..}
        (routes/router ..req..) => ..rt..
        (core/get-entities ..auth.. ..db.. ..org.. [..fileid..] ..rt..) => (seq [file-entity])
        (core/get-entities ..auth.. ..db.. ..org.. [..id2..] ..rt..) => (seq [source-entity])
        (core/create-entities ..auth.. ..db.. ..org.. [{:type "Source" :attributes {}}] ..rt.. :parent ..fileid..) => [source-entity]
        (core/create-values ..auth.. ..db.. ..rt.. ..org.. anything) => ..links..
        (core/update-entities ..auth.. ..db.. ..org.. anything ..rt.. :authorize false :update-collaboration-roots true) => ..updates..))))

(facts "About move-file*"
  (fact "fails if entity not file or folder"
    (let [src  {:type         "Folder"
                :_id          ..src..
                :organization ..org..}
          file {:type         "Whoa"
                :_id          ..file..
                :organization ..org..}
          dest {:type         "Folder"
                :_id          ..dest..
                :organization ..org..}]

      (r/move-contents* ..req.. ..db.. ..org.. ..file.. {:source ..src.. :destination ..dest..}) => (throws ExceptionInfo)
      (provided
        ..req.. =contains=> {:identity ..auth..}
        (routes/router ..req..) => ..rt..
        (core/get-entities ..auth.. ..db.. ..org.. [..src..] ..rt..) => (seq [src])
        (core/get-entities ..auth.. ..db.. ..org.. [..dest..] ..rt..) => (seq [dest])
        (core/get-entities ..auth.. ..db.. ..org.. [..file..] ..rt..) => (seq [file]))))

  (fact "fails if src not a folder"
    (let [src  {:type         "Whoa"
                :_id          ..src..
                :organization ..org..}
          file {:type         "File"
                :_id          ..file..
                :organization ..org..}
          dest {:type         "Folder"
                :_id          ..dest..
                :organization ..org..}]

      (r/move-contents* ..req.. ..db.. ..org.. ..file.. {:source ..src.. :destination ..dest..}) => (throws ExceptionInfo)
      (provided
        ..req.. =contains=> {:identity ..auth..}
        (routes/router ..req..) => ..rt..
        (core/get-entities ..auth.. ..db.. ..org.. [..src..] ..rt..) => (seq [src])
        (core/get-entities ..auth.. ..db.. ..org.. [..dest..] ..rt..) => (seq [dest])
        (core/get-entities ..auth.. ..db.. ..org.. [..file..] ..rt..) => (seq [file]))))

  (fact "fails if dest not a folder"
    (let [src  {:type         "Folder"
                :_id          ..src..
                :organization ..org..}
          file {:type         "File"
                :_id          ..file..
                :organization ..org..}
          dest {:type         "Whoa"
                :_id          ..dest..
                :organization ..org..}]

      (r/move-contents* ..req.. ..db.. ..org.. ..file.. {:source ..src.. :destination ..dest..}) => (throws ExceptionInfo)
      (provided
        ..req.. =contains=> {:identity ..auth..}
        (routes/router ..req..) => ..rt..
        (core/get-entities ..auth.. ..db.. ..org.. [..src..] ..rt..) => (seq [src])
        (core/get-entities ..auth.. ..db.. ..org.. [..dest..] ..rt..) => (seq [dest])
        (core/get-entities ..auth.. ..db.. ..org.. [..file..] ..rt..) => (seq [file]))))

  (fact "adds relationships"
    (let [src  {:type         "Folder"
                :_id          ..src..
                :organization ..org..}
          file {:type         "File"
                :_id          ..file..
                :organization ..org..}
          dest {:type         "Folder"
                :_id          ..dest..
                :organization ..org..}]

      (r/move-contents* ..req.. ..db.. ..org.. ..file.. {:source ..src.. :destination ..dest..}) => {:file    file
                                                                                                     :links   ..created-links..
                                                                                                     :updates ..updated-entities..}
      (provided
        ..req.. =contains=> {:identity ..auth..}
        (routes/router ..req..) => ..rt..
        (core/get-entities ..auth.. ..db.. ..org.. [..src..] ..rt..) => (seq [src])
        (core/get-entities ..auth.. ..db.. ..org.. [..dest..] ..rt..) => (seq [dest])
        (core/get-entities ..auth.. ..db.. ..org.. [..file..] ..rt..) => (seq [file])
        (links/add-links ..auth.. ..db.. ..org.. [dest] "files" [..file..] ..rt.. :inverse-rel "parents") => {:links   ..links..
                                                                                                              :updates ..updates..}
        (links/delete-links ..auth.. ..db.. ..rt.. ..org.. src "files" ..file..) => ..deleted..
        (core/create-values ..auth.. ..db.. ..rt.. ..org.. ..links..) => ..created-links...
        (core/update-entities ..auth.. ..db.. ..org.. ..updates.. ..rt.. :authorize false :update-collaboration-roots true) => ..updated-entities..))))

