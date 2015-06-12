(ns ovation.schema
  (:import (us.physion.ovation.domain OvationEntity$AnnotationKeys TrashInfo))
  (:require [ring.swagger.schema :refer [field describe]]
            [schema.core :as s]))

;;; --- Schema Definitions --- ;;;

(s/defschema Success {:success s/Bool})


;; -- ANNOTATIONS -- ;;

(def AnnotationBase {:_id                    s/Str
                     :_rev                   s/Str
                     :user                   s/Str
                     :entity                 s/Str
                     (s/optional-key :links) s/Any})

(s/defschema AnnotationTypes (s/enum OvationEntity$AnnotationKeys/TAGS
                               OvationEntity$AnnotationKeys/PROPERTIES
                               OvationEntity$AnnotationKeys/NOTES
                               OvationEntity$AnnotationKeys/TIMELINE_EVENTS))

(s/defschema TagRecord {:tag s/Str})
(s/defschema TagAnnotation (conj AnnotationBase {:annotation_type OvationEntity$AnnotationKeys/TAGS
                                                 :annotation      TagRecord}))

(s/defschema PropertyRecord {:key   s/Str
                             :value (describe s/Str "(may be any JSON type)")})
(s/defschema PropertyAnnotation (conj AnnotationBase {:annotation_type OvationEntity$AnnotationKeys/PROPERTIES
                                                      :annotation      PropertyRecord}))

(s/defschema NoteRecord {:text      s/Str
                         :timestamp s/Str})
(s/defschema NoteAnnotation (conj AnnotationBase {:annotation_type OvationEntity$AnnotationKeys/NOTES
                                                  :annotation      NoteRecord}))

(s/defschema TimelineEventRecord {:name                 s/Str
                                  :notes                s/Str
                                  :start                s/Str
                                  (s/optional-key :end) s/Str})
(s/defschema TimelineEventAnnotation (conj AnnotationBase {:annotation_type OvationEntity$AnnotationKeys/TIMELINE_EVENTS
                                                           :annotation      TimelineEventRecord}))



;; -- LINKS -- ;;
(s/defschema Link {:target_id                    s/Uuid
                   :rel                          s/Str
                   (s/optional-key :inverse_rel) s/Str})

(s/defschema NamedLink (assoc Link :name s/Str))

(s/defschema NewEntityLink {:target_id                    s/Str
                            (s/optional-key :inverse_rel) s/Str})

;; From us.physion.ovation.values.Relation#toMap
(s/defschema LinkInfo {:_id                          s/Str
                       (s/optional-key :_rev)        s/Str
                       :user_id                      s/Uuid
                       :source_id                    s/Uuid
                       :target_id                    s/Uuid
                       :rel                          s/Str
                       (s/optional-key :name)        s/Str
                       (s/optional-key :inverse_rel) s/Str
                       })


;; -- ENTITIES -- ;;

(s/defschema NewEntity {:type       s/Str
                        :attributes {s/Keyword s/Any}})

(s/defschema BaseEntity (assoc NewEntity :_rev s/Str
                                         :_id s/Uuid
                                         (s/optional-key :api_version) s/Int))


(s/defschema Entity (assoc BaseEntity
                      :links {s/Keyword                              s/Str
                              (s/optional-key :_collaboration_roots) [s/Str]}
                      (s/optional-key :named_links) {s/Keyword {s/Keyword s/Str}}))


(s/defschema EntityUpdate BaseEntity)

(s/defschema NewAnalysisRecord
  {:inputs [s/Uuid]
   :outputs [s/Uuid]
   (s/optional-key :parameters) {s/Keyword s/Any}})

;; -- TRASH INFO -- ;;

(s/defschema TrashInfoMap {(keyword TrashInfo/TRASHING_USER) s/Str ;; URI
                           (keyword TrashInfo/TRASHING_DATE) s/Str ;; ISO DateTime
                           (keyword TrashInfo/TRASH_ROOT)    s/Str ;; URI
                           })

(s/defschema TrashedEntity (assoc Entity (s/optional-key :trash_info) TrashInfoMap))



