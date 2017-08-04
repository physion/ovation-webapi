(ns ovation.schema
  (:require [ring.swagger.schema :refer [field describe]]
            [schema.core :as s]
            [ovation.constants :as k]
            [ovation.util :as util]))

(s/defschema Id (s/either s/Int s/Str))

;; -- Json API -- ;;
(s/defschema JsonApiError {:errors {s/Keyword                s/Any
                                    (s/optional-key :detail) s/Str}})


;; -- ANNOTATIONS -- ;;

(def AnnotationBase {:_id                              s/Str
                     :_rev                             s/Str
                     :user                             s/Uuid
                     :entity                           s/Uuid
                     :type                             (s/eq "Annotation")
                     (s/optional-key :organization)    Id
                     (s/optional-key :organization_id) Id
                     (s/optional-key :links)           {(s/optional-key :_collaboration_roots) [s/Str]
                                              s/Keyword s/Str}
                     (s/optional-key :permissions)     {s/Keyword s/Bool}})

(s/defschema AnnotationTypes (s/enum k/TAGS
                                     k/PROPERTIES
                                     k/NOTES
                                     k/TIMELINE_EVENTS))

(s/defschema TagRecord {:tag s/Str})
(s/defschema TagAnnotation (conj AnnotationBase {:annotation_type (s/eq k/TAGS)
                                                 :annotation      TagRecord}))

(s/defschema PropertyRecord {:key   s/Str
                             :value s/Any})
(s/defschema PropertyAnnotation (conj AnnotationBase {:annotation_type (s/eq k/PROPERTIES)
                                                      :annotation      PropertyRecord}))

(s/defschema NoteRecord {:text                             s/Str
                         (s/optional-key :organization_id) Id
                         :timestamp                        s/Str})
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

(s/defschema LinkInfo {:_id                              s/Str
                       (s/optional-key :_rev)            s/Str
                       :type                             (s/eq util/RELATION_TYPE)

                       :user_id                          s/Uuid
                       :source_id                        s/Uuid
                       :target_id                        s/Uuid
                       :rel                              s/Str
                       (s/optional-key :organization)    s/Int
                       (s/optional-key :organization_id) s/Int
                       (s/optional-key :name)            s/Str
                       (s/optional-key :inverse_rel)     s/Str

                       (s/optional-key :attributes)      {s/Keyword s/Any}

                       :links                            {(s/optional-key :_collaboration_roots) [s/Str]
                                                          (s/optional-key :self)                 s/Str}})



;; -- ENTITIES -- ;;

(s/defschema NewEntity {:type       s/Str
                        :attributes {s/Keyword s/Any}
                        (s/optional-key :organization) s/Int})

(s/defschema BaseEntity (assoc NewEntity :_rev s/Str
                                         :_id s/Uuid
                                         (s/optional-key :organization) s/Int
                                         (s/optional-key :organization_id) Id
                                         (s/optional-key :api_version) s/Int
                                         (s/optional-key :permissions) {s/Keyword s/Bool}))


(s/defschema Entity (assoc BaseEntity
                      (s/optional-key :owner) s/Uuid
                      :relationships {s/Keyword {:self    s/Str
                                                 :related s/Str}}

                      ;; For File
                      (s/optional-key :revisions) {s/Uuid {:status                 (s/enum k/UPLOADING k/COMPLETE k/ERROR)
                                                           :started-at             s/Str ;;FIX dash
                                                           (s/optional-key :error) s/Str}}

                      :links {:self                                  s/Str
                              (s/optional-key :team)                s/Str
                              (s/optional-key :heads)                s/Str
                              (s/optional-key :zip)                  s/Str
                              (s/optional-key :tags)                 s/Str
                              (s/optional-key :properties)           s/Str
                              (s/optional-key :notes)                s/Str
                              (s/optional-key :timeline-events)      s/Str ;;FIX dash
                              (s/optional-key :upload-complete)      s/Str ;;FIX dash
                              (s/optional-key :upload-failed)        s/Str ;;FIX dash
                              (s/optional-key :_collaboration_roots) [s/Str]}))


(s/defschema EntityUpdate (-> BaseEntity
                            (dissoc BaseEntity :links :relationships :permissions)))

  ;; -- Entity types --;;
(s/defschema NewProject (-> NewEntity
                            (assoc :type (s/eq "Project"))))
(s/defschema Project (-> Entity
                       (assoc :type (s/eq "Project"))
                       (assoc (s/optional-key :team) s/Int)))
(s/defschema ProjectUpdate (-> EntityUpdate
                             (assoc :type (s/eq "Project"))
                             (assoc (s/optional-key :team) s/Int)))

(s/defschema NewChildActivity (-> NewEntity
                                (assoc :type (s/eq "Activity"))
                                (assoc (s/optional-key :relationships) {(s/optional-key :inputs)  {:related     [s/Str]
                                                                                                   :type        (s/either (s/eq k/REVISION-TYPE)
                                                                                                                  (s/eq k/SOURCE-TYPE))
                                                                                                   :inverse_rel (s/eq "activities")}
                                                                        (s/optional-key :outputs) {:related     [s/Str]
                                                                                                   :type        (s/either (s/eq k/REVISION-TYPE)
                                                                                                                  (s/eq k/SOURCE-TYPE))
                                                                                                   :inverse_rel (s/eq "origins")}
                                                                        (s/optional-key :actions) {:related     [s/Str]
                                                                                                   :type        (s/eq k/REVISION-TYPE)
                                                                                                   :inverse_rel (s/eq "procedures")}})))

(s/defschema NewActivity (-> NewEntity
                           (assoc :type (s/eq "Activity"))
                           (assoc (s/optional-key :relationships) {:parents                  {:related           [s/Str]
                                                                                              :type              (s/eq k/PROJECT-TYPE)
                                                                                              :inverse_rel       (s/eq "activities")
                                                                                              :create_as_inverse (s/either (s/eq true) (s/eq "true"))}
                                                                   (s/optional-key :inputs)  {:related     [s/Str]
                                                                                              :type        (s/either (s/eq k/REVISION-TYPE)
                                                                                                             (s/eq k/SOURCE-TYPE))
                                                                                              :inverse_rel (s/eq "activities")}
                                                                   (s/optional-key :outputs) {:related     [s/Str]
                                                                                              :type        (s/either (s/eq k/REVISION-TYPE)
                                                                                                             (s/eq k/SOURCE-TYPE))
                                                                                              :inverse_rel (s/eq "origins")}
                                                                   (s/optional-key :actions) {:related     [s/Str]
                                                                                              :type        (s/eq k/REVISION-TYPE)
                                                                                              :inverse_rel (s/eq "procedures")}})))

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


;; -- Organizations -- ;;
(s/defschema NewOrganization
  {:type     (s/eq "Organization")
   :name     s/Str
   s/Keyword s/Any})

(s/defschema Organization
  {:id                                        Id
   :type                                      (s/eq "Organization")
   :uuid                                      s/Uuid
   :name                                      s/Str
   (s/optional-key :logo_image)               s/Str
   (s/optional-key :is_admin)                 s/Bool
   (s/optional-key :research_subscription_id) Id
   (s/optional-key :links)                    {:self     s/Str
                                               s/Keyword s/Any}})

(s/defschema NewOrganizationMembership
  {:type                                 (s/eq "OrganizationMembership")
   :organization_id                      Id
   :role                                 s/Str
   :email                                s/Str
   :first_name                           s/Str
   (s/optional-key :last_name)           s/Str
   (s/optional-key :job_title)           s/Str
   (s/optional-key :contact_information) s/Str
   (s/optional-key :links)               {:self s/Str}})


(s/defschema OrganizationMembership
  (-> NewOrganizationMembership
    (assoc :id Id)
    (assoc s/Keyword s/Any)))

;; -- Organization groups -- ;;
(s/defschema NewOrganizationGroup
  {:type                   (s/eq "OrganizationGroup")
   :name                   s/Str
   :organization_id        Id
   (s/optional-key :links) {:self              s/Str
                            :group-memberships s/Str}})

(s/defschema OrganizationGroup
  (-> NewOrganizationGroup
    (assoc (s/optional-key :project_count) s/Int)
    (assoc (s/optional-key :member_count) s/Int)
    (assoc (s/optional-key :team_ids) [s/Uuid])
    (assoc (s/optional-key :organization_group_membership_ids) [Id])
    (assoc :id Id)))


;; -- Organization group memberships -- ;;
(s/defschema NewOrganizationGroupMembership
  {:type                       (s/eq "GroupMembership")
   :organization_membership_id Id
   :organization_group_id      Id
   (s/optional-key :links)     {:self s/Str}})

(s/defschema OrganizationGroupMembership
  (-> NewOrganizationGroupMembership
    (assoc :id Id)))

;; -- Teams -- ;;

(s/defschema NewTeamGroup
  {:team_id               Id
   (s/optional-key :type) s/Str
   :organization_group_id Id
   :role_id               Id})

(s/defschema TeamGroup
  {:id                    Id
   :team_id               Id
   :organization_group_id Id
   :role_id               Id
   (s/optional-key :type) s/Str
   :name                  s/Str})

(s/defschema TeamRole
  {:id                     Id
   :organization_id        Id
   :name                   s/Str
   (s/optional-key :links) {s/Keyword s/Str}})

(s/defschema NewTeamMembership
  {:email s/Str
   :role  TeamRole})

(s/defschema TeamMembership
  {:id                  Id
   :team_id             Id
   :type                s/Str
   :added               s/Str
   :role                TeamRole
   :user_id             s/Int
   s/Keyword            s/Any
   :links               {:self s/Keyword}})

(s/defschema UpdatedTeamMembership
  {:role                                 TeamRole
   s/Keyword                             s/Any
   (s/optional-key :links)               {:self s/Keyword}})

(s/defschema NewTeamRole
  (dissoc TeamRole :links))

(s/defschema NewTeamMembershipRole
  {:email s/Str
   :role  NewTeamRole})

(s/defschema Team
  {:id                                   Id
   :type                                 (s/eq "Team")
   :uuid                                 s/Uuid
   :name                                 s/Str
   :roles                                [TeamRole]
   :memberships                          [TeamMembership]
   :team_groups                          [TeamGroup]
   :links                                {s/Keyword s/Str}
   (s/optional-key :permissions)         {s/Keyword s/Bool}})



;; -- TRASH INFO -- ;;

(s/defschema TrashInfoMap {(keyword k/TRASHING-USER) s/Str  ;; URI
                           (keyword k/TRASHING-DATE) s/Str  ;; ISO DateTime
                           (keyword k/TRASH-ROOT)    s/Str})  ;; URI


(s/defschema TrashedEntity (assoc Entity (s/optional-key :trash_info) TrashInfoMap))

(s/defschema TrashedEntityUpdate (assoc EntityUpdate (s/optional-key :trash_info) TrashInfoMap))

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
