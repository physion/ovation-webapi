(ns ovation.constants)

(def USER-ENTITY "User")
(def ANNOTATION-TYPE "Annotation")

;; Constants from Java API
(def ENTITIES-BY-TYPE-VIEW "entities_by_type")              ;; NB this **MUST** stay in sync with EntityDao$Views/ENTITIES_BY_TYPE
(def LINKS-VIEW "links")                                    ;; NB this **MUST** stay in sync with EntityDao$Views/LINKS
(def ANNOTATIONS-VIEW "annotation_docs")
(def LINK-DOCS-VIEW "link_docs")



(def TRASHING-USER "trashing_user")                         ;; NB this **MUST** stay in sync with TrashInfo
(def TRASHING-DATE "trashing_date")                         ;; NB this **MUST** stay in sync with TrashInfo
(def TRASH-ROOT "trash_root")                               ;; NB this **MUST** stay in sync with TrashInfo


(def PROPERTIES "properties")                               ;; NB this **MUST** staty in sync with AnnotationKeys
(def TAGS "tags")                                           ;; NB this **MUST** staty in sync with AnnotationKeys
(def NOTES "notes")                                         ;; NB this **MUST** staty in sync with AnnotationKeys
(def TIMELINE_EVENTS "timeline_events")                     ;; NB this **MUST** staty in sync with AnnotationKeys
