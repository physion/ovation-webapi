(ns ovation.test.route-helpers
  (:use midje.sweet)
  (:require [ovation.route-helpers :as r]
            [ovation.routes :as routes]
            [ovation.core :as core]
            [ovation.revisions :as revisions]
            [ovation.auth :as auth]
            [ovation.links :as links]
            [ovation.constants :as k]
            [ovation.request-context :as request-context]
            [ovation.transform.serialize :as serialize]
            [ovation.util :as util]
            [ovation.request-context :as request-context])
  (:import (clojure.lang ExceptionInfo)))

(defn sling-throwable
  [exception-map]
  (slingshot.support/get-throwable (slingshot.support/make-context
                                     exception-map
                                     (str "throw+: " map)
                                     nil
                                     (slingshot.support/stack-trace))))

(against-background [(request-context/make-context ..req.. ..org.. anything ..pubsub..) => ..ctx..
                     ..ctx.. =contains=> {::request-context/identity ..auth..
                                          ::request-context/routes   ..rt..}]

  (facts "About get-head-revisions*"
    (fact "returns HEAD revisions for file"
      (r/get-head-revisions* ..req.. ..db.. ..org.. ..authz.. ..pubsub.. ..id..) => {:body {:revisions ..revs..} :headers {} :status 200}
      (provided
        ..req.. =contains=> {:identity ..auth..}
        ..file.. =contains=> {:type "File"}
        (serialize/entities ..revs..) => ..revs..
        (revisions/get-head-revisions ..ctx.. ..db.. ..id..) => ..revs..))

    (fact "+throws not-found if HEADs throws not found"
      (r/get-head-revisions* ..req.. ..db.. ..org.. ..authz.. ..pubsub.. ..id..) => (throws ExceptionInfo)
      (provided
        (revisions/get-head-revisions ..ctx.. ..db.. ..id..) =throws=> (sling-throwable {:type ::revisions/not-found}))))

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
            activity     (-> new-activity
                           (assoc :_id (util/make-uuid)))]
        (:body (r/post-resource* ..ctx.. ..db.. k/PROJECT-TYPE ..projectid.. [new-activity])) => {:entities [activity]
                                                                                                  :links    ..links..
                                                                                                  :updates  ..updates..}
        (provided
          (serialize/entities [activity]) => [activity]
          (serialize/entities ..updates..) => ..updates..
          (serialize/values ..links..) => ..links..
          (r/remove-embedded-relationships [new-activity]) => [(dissoc new-activity :relationships)]
          (core/get-entities ..ctx.. ..db.. [..projectid..]) => [project]
          (core/create-entities ..ctx.. ..db.. [(dissoc new-activity :relationships)] :parent ..projectid..) => [activity]
          (links/add-links ..ctx.. ..db.. [activity] :inputs [(:_id rev)] :inverse-rel :activities) => {:updates ..updates..
                                                                                                        :links   ..embedded..}
          (links/add-links ..ctx.. ..db.. [project] "activities" [(:_id activity)] :inverse-rel "parents") => {:updates ..updates..
                                                                                                               :links   ..links..}
          (core/create-values ..ctx.. ..db.. anything) => ..links..
          (core/update-entities ..ctx.. ..db.. anything :authorize false :update-collaboration-roots true) => ..updates..)))


    (fact "handles embedded relationships with :create_as_inverse == true"
      (let [project      {:type k/PROJECT-TYPE
                          :_id  ..projectid..}
            new-activity {:type          k/ACTIVITY-TYPE
                          :relationships {:parents {:related           [(:_id project)]
                                                    :type              k/PROJECT-TYPE
                                                    :inverse_rel       :activities
                                                    :create_as_inverse true}}}
            activity     (-> new-activity
                           (assoc :_id (util/make-uuid)))]
        (:body (r/post-resource* ..ctx.. ..db.. k/PROJECT-TYPE ..projectid.. [new-activity])) => {:entities [activity]
                                                                                                  :links    ..links..
                                                                                                  :updates  ..updates..}
        (provided
          (serialize/entities [activity]) => [activity]
          (serialize/entities ..updates..) => ..updates..
          (serialize/values ..links..) => ..links..
          (core/get-entities ..ctx.. ..db.. [..projectid..]) => [project]
          (r/remove-embedded-relationships [new-activity]) => [(dissoc new-activity :relationships)]
          (links/add-links ..ctx.. ..db.. anything :activities [(:_id activity)] :inverse-rel :parents) => {:updates ..updates..
                                                                                                            :links   ..embedded..}
          (links/add-links ..ctx.. ..db.. anything "activities" [(:_id activity)] :inverse-rel "parents") => {:updates ..updates..
                                                                                                              :links   ..links..}

          (core/create-entities ..ctx.. ..db.. [(dissoc new-activity :relationships)] :parent ..projectid..) => [activity]
          (core/create-values ..ctx.. ..db.. anything) => ..links..
          (core/update-entities ..ctx.. ..db.. anything :authorize false :update-collaboration-roots true) => ..updates..)))

    (fact "adds relationships for a new Source"
      (let [source-entity {:type "Source"
                           :_id  ..id2..}
            file-entity   {:type "File"
                           :_id  ..fileid..}]
        (:body (r/post-resource* ..ctx.. ..db.. "file" ..fileid.. [{:type       "Source"
                                                                    :attributes {}}])) => {:entities (seq [source-entity])
                                                                                           :links    ..links..
                                                                                           :updates  ..updates..}
        (provided
          (serialize/entities [source-entity]) => [source-entity]
          (serialize/entities ..updates..) => ..updates..
          (serialize/values ..links..) => ..links..
          (core/get-entities ..ctx.. ..db.. [..fileid..]) => (seq [file-entity])
          (core/get-entities ..ctx.. ..db.. [..id2..]) => (seq [source-entity])
          (core/create-entities ..ctx.. ..db.. [{:type "Source" :attributes {}}] :parent ..fileid..) => [source-entity]
          (core/create-values ..ctx.. ..db.. anything) => ..links..
          (core/update-entities ..ctx.. ..db.. anything :authorize false :update-collaboration-roots true) => ..updates..))))

  (facts "About move-file*"
    (fact "fails if entity not file or folder"
      (let [src  {:type "Folder"
                  :_id  ..src..}
            file {:type "Whoa"
                  :_id  ..file..}
            dest {:type "Folder"
                  :_id  ..dest..}]

        (r/move-contents* ..req.. ..db.. ..org.. ..authz.. ..pubsub.. ..file.. {:source ..src.. :destination ..dest..}) => (throws ExceptionInfo)
        (provided
          (core/get-entities ..ctx.. ..db.. [..src..]) => (seq [src])
          (core/get-entities ..ctx.. ..db.. [..dest..]) => (seq [dest])
          (core/get-entities ..ctx.. ..db.. [..file..]) => (seq [file]))))

    (fact "fails if src not a folder"
      (let [src  {:type "Whoa"
                  :_id  ..src..}
            file {:type "File"
                  :_id  ..file..}
            dest {:type "Folder"
                  :_id  ..dest..}]

        (r/move-contents* ..req.. ..db.. ..org.. ..authz.. ..pubsub.. ..file.. {:source ..src.. :destination ..dest..}) => (throws ExceptionInfo)
        (provided
          (core/get-entities ..ctx. ..db.. [..src..]) => (seq [src])
          (core/get-entities ..ctx. ..db.. [..dest..]) => (seq [dest])
          (core/get-entities ..ctx. ..db.. [..file..]) => (seq [file]))))

    (fact "fails if dest not a folder"
      (let [src  {:type "Folder"
                  :_id  ..src..}
            file {:type "File"
                  :_id  ..file..}
            dest {:type "Whoa"
                  :_id  ..dest..}]

        (r/move-contents* ..req.. ..db.. ..org.. ..authz.. ..pubsub.. ..file.. {:source ..src.. :destination ..dest..}) => (throws ExceptionInfo)
        (provided
          (core/get-entities ..ctx.. ..db.. [..src..]) => (seq [src])
          (core/get-entities ..ctx.. ..db.. [..dest..]) => (seq [dest])
          (core/get-entities ..ctx.. ..db.. [..file..]) => (seq [file]))))

    (fact "adds relationships"
      (let [src  {:type         "Folder"
                  :_id          ..src..}

            file {:type         "File"
                  :_id          ..file..}

            dest {:type         "Folder"
                  :_id          ..dest..}]


        (r/move-contents* ..req.. ..db.. ..org.. ..authz.. ..pubsub.. ..file.. {:source ..src.. :destination ..dest..}) => {:file    file
                                                                                                       :links   ..created-links..
                                                                                                       :updates ..updated-entities..}
        (provided
          (core/get-entities ..ctx.. ..db.. [..src..]) => (seq [src])
          (core/get-entities ..ctx.. ..db.. [..dest..]) => (seq [dest])
          (core/get-entities ..ctx.. ..db.. [..file..]) => (seq [file])
          (links/add-links ..ctx.. ..db.. [dest] "files" [..file..] :inverse-rel "parents") => {:links   ..links..
                                                                                                :updates ..updates..}
          (links/delete-links ..ctx.. ..db.. src "files" ..file..) => ..deleted..
          (core/create-values ..ctx.. ..db.. ..links..) => ..created-links...
          (core/update-entities ..ctx.. ..db.. ..updates.. :authorize false :update-collaboration-roots true) => ..updated-entities..)))))

