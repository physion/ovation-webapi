(ns ovation.test.transform
  (:use midje.sweet)
  (:require [ovation.transform.read :as tr]
            [ovation.transform.write :as tw]
            [ovation.version :refer [version]]
            [ovation.routes :as r]
            [ovation.schema :as s]
            [ovation.util :as util]
            [ovation.constants :as c]
            [ovation.constants :as k]
            [ovation.request-context :as request-context]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [ovation.auth :as auth])
  (:import (clojure.lang ExceptionInfo)))


(against-background [..ctx.. =contains=> {::request-context/identity      ..auth..
                                          :ovation.request-context/routes ..rt..
                                          :ovation.request-context/org    ..org..}
                     (request-context/user-id ..ctx..) => ..owner-id..]
  (facts "About Annotations"
    (fact "adds self link"
      ((tr/couch-to-value ..ctx..) {:type            k/ANNOTATION-TYPE
                                    :annotation_type k/TAGS
                                    :entity          ..id..
                                    :organization    ..org..
                                    :_id             ..annotation..}) => {:type            k/ANNOTATION-TYPE
                                                                          :annotation_type k/TAGS
                                                                          :entity          ..id..
                                                                          :_id             ..annotation..
                                                                          :links           {:self ..self..}}
      (provided
        (tr/add-value-permissions {:type            k/ANNOTATION-TYPE
                                   :annotation_type k/TAGS
                                   :entity          ..id..
                                   :organization_id ..org..
                                   :_id             ..annotation..
                                   :links           {:self ..self..}} ..ctx..) => {:type             k/ANNOTATION-TYPE
                                                                                    :annotation_type k/TAGS
                                                                                    :entity          ..id..
                                                                                    :_id             ..annotation..
                                                                                    :links           {:self ..self..}}
        (r/named-route ..ctx.. :delete-tags {:org ..org.. :id ..id.. :annotation-id ..annotation..}) => ..self..)))

  (facts "About annotation links"
    (fact "adds annotation links to entity"
      (tr/add-annotation-links {:_id   "123"
                                :links {:foo "bar"}} ..ctx..) => {:_id  "123"
                                                                  :links {:foo             "bar"
                                                                          :properties      "/api/v1/entities/123/annotations/properties"
                                                                          :tags            "/api/v1/entities/123/annotations/tags"
                                                                          :notes           "/api/v1/entities/123/annotations/notes"
                                                                          :timeline-events "/api/v1/entities/123/annotations/timeline_events"}}
      (provided
        (r/annotations-route ..ctx.. {:_id  "123"
                                      :links {:foo "bar"}} "tags") => "/api/v1/entities/123/annotations/tags"
        (r/annotations-route ..ctx.. {:_id  "123"
                                      :links {:foo "bar"}} "properties") => "/api/v1/entities/123/annotations/properties"
        (r/annotations-route ..ctx.. {:_id  "123"
                                      :links {:foo "bar"}} "notes") => "/api/v1/entities/123/annotations/notes"
        (r/annotations-route ..ctx.. {:_id  "123"
                                      :links {:foo "bar"}} "timeline_events") => "/api/v1/entities/123/annotations/timeline_events")))

  (facts "About error handling"
    (facts "in couch-to-entity"
      (fact "throws conflict! if any doc has {:error 'conflict'}"
        ((tr/couch-to-entity ..ctx..) {:_id ..id.. :error "conflict"}) => (throws ExceptionInfo))
      (fact "throws forbidden! if any doc has {:error 'forbidden'}"
        ((tr/couch-to-entity ..ctx..) {:_id ..id.. :error "forbidden"}) => (throws ExceptionInfo))
      (fact "throws unauthorized! if any doc has {:error 'unauthorized'}"
        ((tr/couch-to-entity ..ctx..) {:_id ..id.. :error "unauthorized"}) => (throws ExceptionInfo))))

  (facts "About DTO link modifications"
    (fact "`remove-hidden-links` removes '_...' links"
      (tr/remove-private-links {:_id   ...id...
                                :links {"_collaboration_links" #{...hidden...}
                                        :link1                 ...link1...}}) => {:_id   ...id...
                                                                                  :links {:link1 ...link1...}})

    (fact "`add-relationship-links adds rel links for entity type"
      (let [type-rel {:relA {}
                      :relB {}}
            dto      {:type ..type.. :links {:_collaboration_roots [..collab..]}}]
        (tr/add-relationship-links dto ..ctx..) => (-> dto
                                                     (assoc-in [:links] {:_collaboration_roots (get-in dto [:links :_collaboration_roots])})
                                                     (assoc-in [:relationships] {:relA {:self    ..relA-self..
                                                                                        :related ..relA-related..}
                                                                                 :relB {:self    ..relB-self..
                                                                                        :related ..relB-related..}}))
        (provided
          (util/entity-type-keyword dto) => ..type..
          (s/EntityRelationships ..type..) => type-rel
          (r/targets-route ..ctx.. dto :relA) => ..relA-related..
          (r/targets-route ..ctx.. dto :relB) => ..relB-related..
          (r/relationship-route ..ctx.. dto :relA) => ..relA-self..
          (r/relationship-route ..ctx.. dto :relB) => ..relB-self..)))


    (fact "`add-heads-link` adds heads to File entity links"
      (let [type k/FILE-TYPE
            dto  {:type type :links {:_collaboration_roots [..collab..]}}]
        (tr/add-heads-link dto ..ctx..) => (-> dto
                                            (assoc-in [:links] {:_collaboration_roots (get-in dto [:links :_collaboration_roots])
                                                                :heads                ..headrt..}))
        (provided
          (r/heads-route ..ctx.. dto) => ..headrt..)))

    (facts "add-zip-link"
      (fact "adds zip link for Activity"
        (let [type k/ACTIVITY-TYPE
              dto  {:type type :links {:_collaboration_roots [..roots..]}}]
          (tr/add-zip-link dto ..ctx..) => (assoc-in dto [:links :zip] ..zip..)
          (provided
            (r/zip-activity-route ..ctx.. dto) => ..zip..)))

      (fact "adds zip link for Folder"
        (let [type k/FOLDER-TYPE
              dto  {:type type :links {:_collaboration_roots [..roots..]}}]
          (tr/add-zip-link dto ..ctx..) => (assoc-in dto [:links :zip] ..zip..)
          (provided
            (r/zip-folder-route ..ctx.. dto) => ..zip..)))

      (fact "does not add zip link for Project"
        (let [type k/PROJECT-TYPE
              dto  {:type type :links {:_collaboration_roots [..roots..]}}]
          (tr/add-zip-link dto ..rt..) => dto)))

    (fact "`add-self-link` adds self link to entity"
      (let [couch {:_id   ..id..
                   :type  ..type..
                   :links {}}]
        (tr/add-self-link couch ..ctx..) => {:_id      ..id..
                                                :type  ..type..
                                                :links {:self ..route..}}
        (provided
          (r/self-route ..ctx.. couch) => ..route..)))

    (facts "`add-team-link`"
      (fact "adds link for Project"
        (let [couch {:_id ..id..
                     :type k/PROJECT-TYPE
                     :links {}}]
          (tr/add-team-link couch ..ctx..) => (assoc-in couch [:links :team] ..team..)
          (provided
            (r/team-route ..ctx.. ..id..) => ..team..)))
      (fact "does not add team link for non-project"
        (let [couch {:_id ..id..
                     :type k/SOURCE-TYPE
                     :links {}}]
          (tr/add-team-link couch ..ctx..) => couch)))

    (fact "`couch-to-value` adds self link to LinkInfo"
      (let [couch                 {:_id          ..id..
                                   :type         util/RELATION_TYPE
                                   :organization ..org..}
            expected-intermediate (-> couch
                                    (dissoc :organization)
                                    (assoc :organization_id ..org..))]
        ((tr/couch-to-value ..ctx..) couch) => (assoc-in expected-intermediate [:links :self] ..url..)
        (provided
          (util/entity-type-name couch) => c/RELATION-TYPE-NAME
          (r/self-route ..ctx.. expected-intermediate) => ..url..))))

  (facts "About doc-to-couch"
    (fact "skips docs without :type"
      (let [other {:_id "bar" :rel "some-rel"}]
        (tw/doc-to-couch ..ctx.. ..roots.. other) => other))

    (fact "skips docs of type Relation"
      (let [other {:_id "bar" :rel "some-rel" :type "Relation"}]
        (tw/doc-to-couch ..ctx.. ..roots.. other) => other))

    (fact "adds updated_at date"
      (let [doc {:type ..type.. :attributes {:label ..label..}}]
        (:attributes (tw/doc-to-couch ..ctx.. ..roots.. doc)) => (contains {:updated-at ..time..})
        (provided
          (f/unparse (f/formatters :date-time) anything) => ..time..)))

    (fact "adds created_at date"
      (let [doc {:type ..type.. :attributes {:label ..label..}}]
        (:attributes (tw/doc-to-couch ..ctx.. ..roots.. doc)) => (contains {:created-at ..time..})
        (provided
          (f/unparse (f/formatters :date-time) (t/now)) => ..time..)))

    (fact "does not add created_at date if already present"
      (let [doc {:type ..type.. :attributes {:label ..label.. :created-at ..old..}}]
        (get-in (tw/doc-to-couch ..ctx.. ..roots.. doc) [:attributes :created-at]) => ..old..
        (provided
          (f/unparse (f/formatters :date-time) (t/now)) => ..time..)))

    (fact "adds collaboration roots"
      (let [doc {:type ..type.. :attributes {:label ..label..}}]
        (get-in (tw/doc-to-couch ..ctx.. ..roots.. doc) [:links :_collaboration_roots]) => ..roots..)))


  (facts "About `ensure-owner`"
    (fact "it adds owner element"
      (let [doc {:type ..type.. :attributes {:label ..label..}}]
        (tw/ensure-owner doc ..owner..) => (assoc doc :owner ..owner..)))
    (fact "it does not add nil owner"
      (tw/ensure-owner ..doc.. nil) => ..doc..))

  (facts "About add-organization"
    (fact "adds organization from request context"
      (let [doc {:type ..type.. :attributes {:label ..value..}}]
        (tw/add-organization doc ..ctx..) => (assoc doc :organization ..org..)
        (provided
          ..ctx.. =contains=> {::request-context/org ..org..}))))

  (facts "About `remove-user-attributes`"
    (fact "Removes User entity attributes"
      (let [user {:type "User" :attributes {:password ..secret..}}]
        (tr/remove-user-attributes user) => (assoc user :attributes {})))

    (fact "Retains User :name attribute"
      (let [user {:type "User" :attributes {:password ..secret.. :name ..name..}}]
        (tr/remove-user-attributes user) => (assoc user :attributes {:name ..name..})))

    (fact "Does not remove other entity attributes"
      (let [doc {:type "MyEntity" :attributes {:label ..label..}}]
        (tr/remove-user-attributes doc) => doc)))

  (facts "About permissions"
    (against-background [..ctx.. =contains=> {::request-context/identity ..auth..
                                              ::request-context/org      ..org..}])
    (facts "for entities"
      (let [doc {:owner ..id..}]
        (fact "add-entity-permissions sets {update: (can? :update) delete: (can? :delete)}"
          (tr/add-entity-permissions doc ..id..) => (assoc doc :permissions {:update ..update..
                                                                                       :delete ..delete..
                                                                                       :create true})
          (provided
            (auth/can? ..id.. :ovation.auth/update doc) => ..update..
            (auth/can? ..id.. :ovation.auth/delete doc) => ..delete..))))
    (facts "for annotations"
      (let [doc {:user ..id..
                 :type "Annotation"}]
        (fact "add-value-permissions sets {update: (can? :update) delete: (can? :delete)"
          (tr/add-value-permissions doc ..ctx..) => (assoc doc :permissions {:update  true
                                                                              :delete true})
          (provided
            (auth/authenticated-user-id ..auth..) => ..id..
            (auth/organization-ids ..auth..) => [..org..]
            (auth/has-scope? ..auth.. k/WRITE-GLOBAL-SCOPE) => true)))))

  (facts "About entities-from-couch"
    (fact "removes unauthorized documents"
      (tr/entities-from-couch [..doc.. ..bad..] ..ctx..) => [..doc..]
      (provided
        (tr/couch-to-entity ..ctx..) => (fn [doc] doc)
        (auth/authenticated-teams ..auth..) => ..teams..
        (auth/can? ..ctx.. ::auth/read ..doc.. :teams ..teams..) => true
        (auth/can? ..ctx.. ::auth/read ..bad.. :teams ..teams..) => false))))
