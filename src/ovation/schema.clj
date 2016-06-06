(ns ovation.schema
  (:require [ring.swagger.schema :refer [field describe]]
            [schema.core :as s]
            [ovation.constants :as k]
            [ovation.util :as util])
  (:import (org.junit.rules TestName)))

;; -- Json API -- ;;
(s/defschema JsonApiError {:errors {s/Keyword                s/Any
                                    (s/optional-key :detail) s/Str}})


;; -- ANNOTATIONS -- ;;

(def AnnotationBase {:_id                    s/Str
                     :_rev                   s/Str
                     :user                   s/Uuid
                     :entity                 s/Uuid
                     :type                   (s/eq "Annotation")
                     (s/optional-key :links) {(s/optional-key :_collaboration_roots) [s/Str]
                                              s/Keyword s/Str}
                     (s/optional-key :permissions) {s/Keyword s/Bool}})

(s/defschema AnnotationTypes (s/enum k/TAGS
                                     k/PROPERTIES
                                     k/NOTES
                                     k/TIMELINE_EVENTS))

(s/defschema TagRecord {:tag s/Str})
(s/defschema TagAnnotation (conj AnnotationBase {:annotation_type (s/eq k/TAGS)
                                                 :annotation      TagRecord}))

(s/defschema PropertyRecord {:key   s/Str
                             :value (describe s/Str "(may be any JSON type)")})
(s/defschema PropertyAnnotation (conj AnnotationBase {:annotation_type (s/eq k/PROPERTIES)
                                                      :annotation      PropertyRecord}))

(s/defschema NoteRecord {:text      s/Str
                         :timestamp s/Str})
(s/defschema NoteAnnotation (conj AnnotationBase {:annotation_type (s/eq k/NOTES)
                                                  :annotation      NoteRecord
                                                  (s/optional-key :edited_at) s/Str}))

(s/defschema TimelineEventRecord {:name                 s/Str
                                  :notes                s/Str
                                  :start                s/Str
                                  (s/optional-key :end) s/Str})
(s/defschema TimelineEventAnnotation (conj AnnotationBase {:annotation_type (s/eq k/TIMELINE_EVENTS)
                                                           :annotation      TimelineEventRecord}))



;; -- LINKS -- ;;
(s/defschema NewLink {:target_id                    s/Uuid
                      (s/optional-key :inverse_rel) s/Str
                      (s/optional-key :name)        s/Str})

(s/defschema LinkInfo {:_id                          s/Str
                       (s/optional-key :_rev)        s/Str
                       :type                         (s/eq util/RELATION_TYPE)

                       :user_id                      s/Uuid
                       :source_id                    s/Uuid
                       :target_id                    s/Uuid
                       :rel                          s/Str
                       (s/optional-key :name)        s/Str
                       (s/optional-key :inverse_rel) s/Str

                       (s/optional-key :attributes)  {s/Keyword s/Any}

                       :links                        {(s/optional-key :_collaboration_roots) [s/Str]
                                                      (s/optional-key :self)                 s/Str}})



;; -- ENTITIES -- ;;

(s/defschema NewEntity {:type       s/Str
                        :attributes {s/Keyword s/Any}})

(s/defschema BaseEntity (assoc NewEntity :_rev s/Str
                                         :_id s/Uuid
                                         (s/optional-key :api_version) s/Int
                                         (s/optional-key :permissions) {s/Keyword s/Bool}))


(s/defschema Entity (assoc BaseEntity
                      (s/optional-key :owner) s/Uuid
                      :relationships {s/Keyword {
                                                 :self    s/Str
                                                 :related s/Str}}

                      :links {:self                                  s/Str
                              (s/optional-key :heads)                s/Str
                              (s/optional-key :tags)                 s/Str
                              (s/optional-key :properties)           s/Str
                              (s/optional-key :notes)                s/Str
                              (s/optional-key :timeline-events)      s/Str
                              (s/optional-key :_collaboration_roots) [s/Str]}))


(s/defschema EntityUpdate (-> BaseEntity
                            (dissoc BaseEntity :links :relationships :permissions)))

  ;; -- Entity types --;;
(s/defschema NewProject (-> NewEntity
                            (assoc :type (s/eq "Project"))))
(s/defschema Project (-> Entity
                         (assoc :type (s/eq "Project"))))
(s/defschema ProjectUpdate (-> EntityUpdate
                               (assoc :type (s/eq "Project"))))

(s/defschema NewActivity (-> NewEntity
                           (assoc :type (s/eq "Activity"))))
(s/defschema Activity (-> Entity
                        (assoc :type (s/eq "Activity"))))
(s/defschema ActivityUpdate (-> EntityUpdate
                              (assoc :type (s/eq "Activity"))))

(s/defschema NewSource (-> NewEntity
                           (assoc :type (s/eq "Source"))))
(s/defschema Source (-> Entity
                        (assoc :type (s/eq "Source"))))
(s/defschema SourceUpdate (-> EntityUpdate
                              (assoc :type (s/eq "Source"))))


(s/defschema NewFolder (-> NewEntity
                           (assoc :type (s/eq "Folder"))))
(s/defschema Folder (-> Entity
                        (assoc :type (s/eq "Folder"))))
(s/defschema FolderUpdate (-> EntityUpdate
                              (assoc :type (s/eq "Folder"))))

(s/defschema NewFile (-> NewEntity
                         (assoc :type (s/eq "File"))))
(s/defschema File (-> Entity
                      (assoc :type (s/eq "File"))))

(s/defschema FileUpdate (-> EntityUpdate
                            (assoc :type (s/eq "File"))))

(s/defschema NewRevision (-> NewEntity
                             (assoc :type (s/eq "Revision"))
                             (assoc :attributes {:content_type             s/Str
                                                 :name                     s/Str
                                                 (s/optional-key :url)     s/Str
                                                 (s/optional-key :version) s/Str
                                                 s/Keyword                 s/Any})))

(s/defschema Revision (-> Entity
                          (assoc :type (s/eq "Revision"))
                          (assoc :attributes {:content_type             s/Str
                                              :url                      s/Str
                                              :name                     s/Str
                                              (s/optional-key :version) s/Str
                                              :previous                 [s/Uuid]
                                              :file_id                  s/Uuid
                                              s/Keyword                 s/Any})))
(s/defschema RevisionUpdate (-> EntityUpdate
                                (assoc :type (s/eq "Revision"))
                                (assoc :attributes {:content_type             s/Str
                                                    :url                      s/Str
                                                    :name                     s/Str
                                                    (s/optional-key :version) s/Str
                                                    :previous                 [s/Uuid]
                                                    :file_id                  s/Uuid
                                                    s/Keyword                 s/Any})))

(s/defschema CreateRevisionResponse {:entities [Revision]
                                     :links     [LinkInfo]
                                     :updates   [Entity]
                                     :aws       [{:id  s/Str
                                                  :aws {s/Keyword s/Any}}]})

(s/defschema User (-> Entity
                      (assoc :type (s/eq "User"))))

;; -- Teams -- ;;

(s/defschema TeamRole
  {:id                     (s/either s/Str s/Int)
   :organization_id        (s/either s/Str s/Int)
   :name                   s/Str
   (s/optional-key :links) {s/Keyword s/Str}})

(s/defschema TeamMembership
  {:id                     (s/either s/Str s/Int)
   :team_id                (s/either s/Str s/Int)
   :type                   s/Str
   :added                  s/Str
   :role                   TeamRole
   (s/optional-key :email) s/Str
   (s/optional-key :name)  s/Str
   :user_id                s/Int
   :membership_role_ids    [s/Int]
   :links                  {:self s/Keyword}})


(s/defschema PendingTeamMembership
  {:id        (s/either s/Str s/Int)
   :role      TeamRole
   :added     s/Str
   (s/optional-key :email)      s/Str
   (s/optional-key :name) s/Str
   :type      s/Str})

(s/defschema NewTeamRole
  (dissoc TeamRole :links))

(s/defschema NewTeamMembershipRole
  {:email s/Str
   :role  NewTeamRole})

(s/defschema TeamUser
  {
   :id                        s/Int
   :uuid                      s/Uuid
   :name                      s/Str
   :email                     s/Str
   (s/optional-key :location) s/Str
   :links                     {:roles s/Str}
   :type                      (s/eq "User")})


(s/defschema TeamMembershipRole
  {:id            s/Int
   :membership_id s/Int
   :role_id       s/Int})

(s/defschema Team
  {:id                  (s/either s/Str s/Int)
   :type                (s/eq "Team")
   :uuid                s/Uuid
   :name                s/Str
   :roles               [TeamRole]
   :pending_memberships [PendingTeamMembership]
   :memberships         [TeamMembership]
   :links               {s/Keyword s/Str}
   (s/optional-key :permissions) {s/Keyword s/Bool}})



;; -- TRASH INFO -- ;;

(s/defschema TrashInfoMap {(keyword k/TRASHING-USER) s/Str  ;; URI
                           (keyword k/TRASHING-DATE) s/Str  ;; ISO DateTime
                           (keyword k/TRASH-ROOT)    s/Str})  ;; URI


(s/defschema TrashedEntity (assoc Entity (s/optional-key :trash_info) TrashInfoMap))

;; -- Relationships -- ;;

(def EntityChildren                                         ;; relationships to create when posting a child to a parent entity
  {:project  {:folder   {:rel         "folders"
                         :inverse-rel "parents"}
              :file     {:rel         "files"
                         :inverse-rel "parents"}
              :activity {:rel         "activities"
                         :inverse-rel "parents"}}

   :folder   {:folder   {:rel         "folders"
                         :inverse-rel "parents"}
              :file     {:rel         "files"
                         :inverse-rel "parents"}
              :activity {:rel         "activities"
                         :inverse-rel "parents"}}

   :source   {:source {:rel         "children"
                       :inverse-rel "parents"}}

   :file     {:revision {:rel         "revisions"
                         :inverse-rel "file"}
              :source   {:rel         "sources"
                         :inverse-rel "files"}}})

(def EntityRelationships                                    ;; rels to put into entity links at read
  {:project  {:folders    {:schema Folder}
              :files      {:schema File}
              :activities {:schema Activity}}

   :source   {:children  {:schema Source}
              :parents   {:schema Source}
              :files     {:schema File}
              :revisions {:schema Revision}
              :activities {:schema Activity}
              :origins    {:schema Activity}}

   :activity {:inputs  {:schema Entity}                     ; should be Revision or Source
              :outputs {:schema Entity}                     ; should be Revision or Source
              :actions {:schema Revision}
              :parents {:schema Project}}

   :folder   {:folders    {:schema Folder}
              :parents    {:schema Entity}
              :files      {:schema File}
              :activities {:schema Activity}}

   :file     {:revisions {:schema Revision}
              :head      {:schema Revision}
              :parents   {:schema Entity}
              :sources   {:schema Source}}

   :revision {:file       {:schema File}
              :activities {:schema Activity}
              :origins    {:schema Activity}
              :procedures {:schema Activity}}})


