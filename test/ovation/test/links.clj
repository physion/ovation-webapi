(ns ovation.test.links
  (:use midje.sweet)
  (:require [ovation.links :as links]
            [ovation.couch :as couch]
            [ovation.util :as util]
            [ovation.auth :as auth]
            [ovation.core :as core]
            [ovation.version :as ver]
            [ovation.constants :as k]
            [ovation.transform.read :as tr])
  (:import (java.util UUID)))

(facts "About links"
  (against-background [..ctx.. =contains=> {:ovation.request-context/auth   ..auth..
                                            :ovation.request-context/routes ..rt..}]

    (facts "`get-link-targets`"
      (let [doc1 {:attributes {:label ..label1..}}
            doc2 {:attributes {:label ..label2..}}
            doc3 {:attributes {}}]
        (against-background [(couch/get-view ..ctx.. ..db.. k/LINKS-VIEW {:startkey      [..id.. ..rel..]
                                                                          :endkey        [..id.. ..rel..]
                                                                          :inclusive_end true
                                                                          :reduce        false
                                                                          :include_docs  true}) => [doc1 doc2 doc3]
                             (couch/get-view ..ctx.. ..db.. k/LINKS-VIEW {:startkey      [..id.. ..rel.. ..name..]
                                                                          :endkey        [..id.. ..rel.. ..name..]
                                                                          :inclusive_end true
                                                                          :reduce        false
                                                                          :include_docs  true}) => [doc1]
                             (auth/can? anything ::auth/update anything) => true
                             (auth/authenticated-teams ..auth..) => []
                             (ovation.request-context/team-ids ..ctx..) => []
                             (ovation.request-context/user-id ..ctx..) => ..user..
                             (auth/can? anything ::auth/read anything :teams anything) => true
                             (tr/couch-to-entity ..ctx..) => (fn [doc] doc)]

          (fact "gets entity rel targets"
            (links/get-link-targets ..ctx.. ..db.. ..id.. ..rel..) => [doc1 doc2 doc3])

          (fact "gets named entity rel targets"
            (links/get-link-targets ..ctx.. ..db.. ..id.. ..rel.. :name ..name..) => [doc1])

          (fact "filters by label"
            (links/get-link-targets ..ctx.. ..db.. ..id.. ..rel.. :label ..label1..) => [doc1]))))


    (facts "`add-link`"
      (let [doc        {:_id   (str (UUID/randomUUID))
                        :type  "MyEntity" `:attrbutes {}
                        :links {:_collaboration_roots [..sourceroot..]}}
            target-id  (str (UUID/randomUUID))
            target-id2 (str (UUID/randomUUID))]

        (against-background [(core/get-entities ..ctx.. ..db.. [target-id]) => [{:_id   target-id
                                                                                 :type  "not-a-root"
                                                                                 :links {:_collaboration_roots [..targetroot..]}}]
                             (auth/authenticated-user-id ..auth..) => ..id..]


          (fact "creates link document"
            (:links (links/add-links ..ctx.. ..db.. [doc] ..rel.. [target-id] :inverse-rel ..inverse..)) => (contains {:_id         (format "%s--%s-->%s" (:_id doc) ..rel.. target-id)
                                                                                                                       :user_id     ..id..
                                                                                                                       :type        "Relation"
                                                                                                                       :source_id   (:_id doc)
                                                                                                                       :target_id   target-id
                                                                                                                       :rel         (clojure.core/name ..rel..)
                                                                                                                       :inverse_rel (clojure.core/name ..inverse..)
                                                                                                                       :links       {:_collaboration_roots [..sourceroot.. ..targetroot..]}}))

          (fact "creates link document without inverse"
            (:links (links/add-links ..ctx.. ..db.. [doc] ..rel.. [target-id])) => (contains {:_id       (format "%s--%s-->%s" (:_id doc) ..rel.. target-id)
                                                                                              :user_id   ..id..
                                                                                              `:type     "Relation"
                                                                                              :source_id (:_id doc)
                                                                                              :target_id target-id
                                                                                              :rel       (clojure.core/name ..rel..)
                                                                                              :links     {:_collaboration_roots [..sourceroot.. ..targetroot..]}}))
          (fact "creates named link document"
            (:links (links/add-links ..ctx.. ..db.. [doc] ..rel.. [target-id] :inverse-rel ..inverse.. :name ..name..)) => (contains {:_id         (format "%s--%s>%s-->%s" (:_id doc) ..rel.. ..name.. target-id)
                                                                                                                                      :user_id     ..id..
                                                                                                                                      :type        "Relation"
                                                                                                                                      :source_id   (:_id doc)
                                                                                                                                      :target_id   target-id
                                                                                                                                      :name        ..name..
                                                                                                                                      :inverse_rel (clojure.core/name ..inverse..)
                                                                                                                                      :rel         (clojure.core/name ..rel..)
                                                                                                                                      :links       {:_collaboration_roots [..sourceroot.. ..targetroot..]}}))



          (fact "updates source _collaboration_roots by aggregating multiple target collaboration roots"
            (let [rel            "some-rel"
                  source         {:_id "src" :type "Entity" :links {:_collaboration_roots [..roots1..]}}
                  target1        {:_id "target1" :type "Project" :links {:_collaboration_roots [..roots2..]}}
                  target2        {:_id "target2" :type "Project" :links {:_collaboration_roots [..roots3..]}}
                  updated-source (assoc-in source [:links :_collaboration_roots] #{..roots3.. ..roots2.. ..roots1..})]
              (:updates (links/add-links ..ctx.. ..db.. [source] rel [target-id target-id2])) => (contains updated-source)
              (provided
                (core/get-entities ..ctx.. ..db.. [target-id target-id2]) => [target1 target2])))




          (fact "updates target _collaboration_roots for source:=Project"
            (let [rel      "some-rel"
                  source   {:type "Project" :links {:_collaboration_roots nil} :_id ..proj-id..}
                  target   {:type "Entity" :links {:_collaboration_roots [..roots1..]} :_id target-id}
                  expected (assoc-in target [:links :_collaboration_roots] #{..proj-id.. ..roots1..})]
              (:updates (links/add-links ..ctx.. ..db.. [source] rel [target-id])) => (contains expected)
              (provided
                (core/get-entities ..ctx.. ..db.. [target-id]) => [target])))

          (fact "updates target _collaboration_roots for source:=Folder"
            (let [rel      "some-rel"
                  source   {:type "Folder" :links {:_collaboration_roots [..roots1..]}}
                  target   {:type "Entity" :links {:_collaboration_roots [..roots2..]}}
                  expected (assoc-in target [:links :_collaboration_roots] #{..roots2.. ..roots1..})]
              (:updates (links/add-links ..ctx.. ..db.. [source] rel [target-id])) => (contains expected)
              (provided
                (core/get-entities ..ctx.. ..db.. [target-id]) => [target])))

          (fact "updates target _collaboration_roots for source:=File"
            (let [rel      "some-rel"
                  source   {:type "File" :links {:_collaboration_roots [..roots1..]}}
                  target   {:type "Entity" :links {:_collaboration_roots [..roots2..]}}
                  expected (assoc-in target [:links :_collaboration_roots] #{..roots2.. ..roots1..})]
              (:updates (links/add-links ..ctx.. ..db.. [source] rel [target-id])) => (contains expected)
              (provided
                (core/get-entities ..ctx.. ..db.. [target-id]) => [target])))

          (fact "updates source _collaboration_roots for target:=Project"
            (let [rel                "some-rel"
                  source-collab-root (str (UUID/randomUUID))
                  source             {:_id (str (UUID/randomUUID)) :type "Entity" :links {:_collaboration_roots [source-collab-root]}}
                  target             {:_id target-id :type "Project" :links {:_collaboration_roots nil}}
                  expected           (assoc-in source [:links :_collaboration_roots] #{source-collab-root target-id})]
              (:updates (links/add-links ..ctx.. ..db.. [source] rel [target-id])) => (contains expected)
              (provided
                (core/get-entities ..ctx.. ..db.. [target-id]) => [target])))

          (fact "updates source _collaboration_roots for target:=File"
            (let [rel      "some-rel"
                  source   {:_id (str (UUID/randomUUID)) :type "Entity" :links {:_collaboration_roots [..roots1..]}}
                  target   {:_id target-id :type "File" :links {:_collaboration_roots [..roots2..]}}
                  expected (assoc-in source [:links :_collaboration_roots] #{..roots1.. ..roots2..})]
              (:updates (links/add-links ..ctx.. ..db.. [source] rel [target-id])) => (contains expected)
              (provided
                (core/get-entities ..ctx.. ..db.. [target-id]) => [target])))

          (fact "updates source _collaboration_roots for target:=Folder"
            (let [rel      "some-rel"
                  source   {:_id (str (UUID/randomUUID)) :type "Entity" :links {:_collaboration_roots [..roots1..]}}
                  target   {:_id target-id :type "Folder" :links {:_collaboration_roots [..roots2..]}}
                  expected (assoc-in source [:links :_collaboration_roots] #{..roots1.. ..roots2..})]
              (:updates (links/add-links ..ctx.. ..db.. [source] rel [target-id])) => (contains expected)
              (provided
                (core/get-entities ..ctx.. ..db.. [target-id]) => [target]))))))

    (facts "`delete-link`"

      (let [target-id (str (UUID/randomUUID))
            doc       {:_id       (str (UUID/randomUUID))
                       :type      "MyEntity"
                       :attrbutes {}
                       :links     {:_collaboration_roots [target-id]}}]

        (against-background []

          (fact "removes link"
            (let [source-id (:_id doc)
                  link-id   (format "%s--%s-->%s" source-id ..rel.. ..target..)]
              (links/delete-links ..ctx.. ..db.. doc ..rel.. ..target..) => ..deleted..
              (provided
                (core/delete-values ..ctx.. ..db.. [link-id]) => ..deleted..
                (auth/can? ..auth.. ::auth/update doc) => true)))

          (fact "fails if not can? :update source"
            (links/delete-links ..ctx.. ..db.. ..doc.. ..rel.. ..target..) => (throws Exception)
            (provided
              (auth/can? ..auth.. ::auth/update ..doc..) => false))


          (fact "updates entity _collaboration_roots"
            (let [source-id (:_id doc)
                  link-id   (format "%s--%s-->%s" source-id ..rel.. ..target..)]
              (links/delete-links ..ctx.. ..db.. doc ..rel.. ..target..) => ..deleted..
              (provided
                (core/delete-values ..ctx.. ..db.. [link-id]) => ..deleted..
                (auth/can? ..auth.. ::auth/update doc) => true))))))

    (facts "`get-links`"
      (against-background [(couch/get-view ..ctx.. ..db.. k/LINK-DOCS-VIEW {:startkey      [..id.. ..rel..]
                                                                            :endkey        [..id.. ..rel..]
                                                                            :inclusive_end true
                                                                            :reduce        false
                                                                            :include_docs  true}) => ..docs..
                           (tr/values-from-couch ..docs.. ..ctx..) => ..values..]

        (fact "gets relationship documents"
          (links/get-links ..ctx.. ..db.. ..id.. ..rel..) => ..values..)))

    (facts "`collaboration-roots`"
      (fact "returns _collaboration_roots"
        (links/collaboration-roots {:links {:_collaboration_roots [..root..]}}) => [..root..])
      (fact "returns _id if no roots"
        (links/collaboration-roots {:_id ..id.. :links {:foo ..foo..}}) => [..id..])
      (fact "returns _id if empty roots"
        (links/collaboration-roots {:_id ..id.. :links {:_collaboration_roots []}}) => [..id..])
      (fact "returns empty if include-self is false"
        (links/collaboration-roots {:_id ..id.. :links {:_collaboration_roots []}} :include-self false) => []))))
