(ns ovation.constants
  (:require [ovation.util :as util]))


(def READ-GLOBAL-SCOPE "read:global")
(def WRITE-GLOBAL-SCOPE "write:global")

(def ACTIVITY-TYPE "Activity")
(def ANNOTATION-TYPE "Annotation")
(def FILE-TYPE "File")
(def FOLDER-TYPE "Folder")
(def ORGANIZATION-TYPE "Organization")
(def PROJECT-TYPE "Project")
(def RELATION-TYPE "Relation")
(def REVISION-TYPE "Revision")
(def SOURCE-TYPE "Source")
(def TEAM-TYPE "Team")
(def USER-ENTITY "User")

(def TEAM-GROUP-TYPE "TeamGroup")

;; Important rels
(def ACTIVITIES-REL "activities")
(def INPUTS-REL "inputs")
(def OUTPUTS-REL "outputs")
(def ACTIONS-REL "actions")
(def OPERATORS-REL "operators")
(def ORIGINS-REL "origins")
(def PARENTS-REL "parents")

;; Constants from Java API
(def ENTITIES-BY-TYPE-VIEW "entities_by_type")              ;; NB this **MUST** stay in sync with EntityDao$Views/ENTITIES_BY_TYPE
(def LINKS-VIEW "links")                                    ;; NB this **MUST** stay in sync with EntityDao$Views/LINKS
(def ANNOTATIONS-VIEW "annotation_docs")
(def LINK-DOCS-VIEW "link_docs")
(def REVISIONS-VIEW "revisions")
(def ALL-DOCS-VIEW "all_docs")
(def REVISION-BYTES-VIEW "revision-bytes")



(def TRASHING-USER "trashing_user")                         ;; NB this **MUST** stay in sync with TrashInfo
(def TRASHING-DATE "trashing_date")                         ;; NB this **MUST** stay in sync with TrashInfo
(def TRASH-ROOT "trash_root")                               ;; NB this **MUST** stay in sync with TrashInfo


(def PROPERTIES "properties")                               ;; NB this **MUST** staty in sync with AnnotationKeys
(def TAGS "tags")                                           ;; NB this **MUST** staty in sync with AnnotationKeys
(def NOTES "notes")                                         ;; NB this **MUST** staty in sync with AnnotationKeys
(def TIMELINE_EVENTS "timeline_events")                     ;; NB this **MUST** staty in sync with AnnotationKeys

;; Notifications
(def MENTION_NOTIFICATION "mention")

;; Revision status
(def UPLOADING "in-progress")
(def COMPLETE "complete")
(def ERROR "error")

;; Role names
(def ADMIN-ROLE "Admin")
(def CURATOR-ROLE "Curator")
(def MEMBER-ROLE "Member")

;; Rails resources
(def TEAM-GROUPS "team_groups")
