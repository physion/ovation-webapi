(ns ovation.constants
  (:require [ovation.util :as util]))

(def USER-ENTITY "User")
(def ANNOTATION-TYPE "Annotation")
(def REVISION-TYPE "Revision")
(def FILE-TYPE "File")
(def RELATION-TYPE util/RELATION_TYPE)
(def TEAM-TYPE "Team")

(def RELATION-TYPE-NAME (clojure.string/lower-case util/RELATION_TYPE))

;; Constants from Java API
(def ENTITIES-BY-TYPE-VIEW "entities_by_type")              ;; NB this **MUST** stay in sync with EntityDao$Views/ENTITIES_BY_TYPE
(def LINKS-VIEW "links")                                    ;; NB this **MUST** stay in sync with EntityDao$Views/LINKS
(def ANNOTATIONS-VIEW "annotation_docs")
(def LINK-DOCS-VIEW "link_docs")
(def REVISIONS-VIEW "revisions")



(def TRASHING-USER "trashing_user")                         ;; NB this **MUST** stay in sync with TrashInfo
(def TRASHING-DATE "trashing_date")                         ;; NB this **MUST** stay in sync with TrashInfo
(def TRASH-ROOT "trash_root")                               ;; NB this **MUST** stay in sync with TrashInfo


(def PROPERTIES "properties")                               ;; NB this **MUST** staty in sync with AnnotationKeys
(def TAGS "tags")                                           ;; NB this **MUST** staty in sync with AnnotationKeys
(def NOTES "notes")                                         ;; NB this **MUST** staty in sync with AnnotationKeys
(def TIMELINE_EVENTS "timeline_events")                     ;; NB this **MUST** staty in sync with AnnotationKeys
