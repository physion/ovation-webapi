(ns ovation.schema
  (:require [ring.swagger.schema :refer [field describe]]
            [schema.core :as s]
            [ovation.constants :as k]
            [ovation.util :as util]))

;; -- Json API -- ;;
(s/defschema JsonApiError {:errors {s/Keyword s/Any
                                    (s/optional-key :detail) s/Str}})


;; -- ANNOTATIONS -- ;;

(def AnnotationBase {:_id                    s/Str
                     :_rev                   s/Str
                     :user                   s/Uuid
                     :entity                 s/Uuid
                     :type                   "Annotation"
                     (s/optional-key :links) {s/Keyword s/Str}})

(s/defschema AnnotationTypes (s/enum k/TAGS
                                     k/PROPERTIES
                                     k/NOTES
                                     k/TIMELINE_EVENTS))

(s/defschema TagRecord {:tag s/Str})
(s/defschema TagAnnotation (conj AnnotationBase {:annotation_type k/TAGS
                                                 :annotation      TagRecord}))

(s/defschema PropertyRecord {:key   s/Str
                             :value (describe s/Str "(may be any JSON type)")})
(s/defschema PropertyAnnotation (conj AnnotationBase {:annotation_type k/PROPERTIES
                                                      :annotation      PropertyRecord}))

(s/defschema NoteRecord {:text      s/Str
                         :timestamp s/Str})
(s/defschema NoteAnnotation (conj AnnotationBase {:annotation_type k/NOTES
                                                  :annotation      NoteRecord}))

(s/defschema TimelineEventRecord {:name                 s/Str
                                  :notes                s/Str
                                  :start                s/Str
                                  (s/optional-key :end) s/Str})
(s/defschema TimelineEventAnnotation (conj AnnotationBase {:annotation_type k/TIMELINE_EVENTS
                                                           :annotation      TimelineEventRecord}))



;; -- LINKS -- ;;
(s/defschema NewLink {:target_id                    s/Uuid
                      (s/optional-key :inverse_rel) s/Str
                      (s/optional-key :name)        s/Str})

(s/defschema LinkInfo {:_id                          s/Str
                       (s/optional-key :_rev)        s/Str
                       :type                         util/RELATION_TYPE

                       :owner                        s/Uuid
                       :source_id                    s/Uuid
                       :target_id                    s/Uuid
                       :rel                          s/Str
                       (s/optional-key :name)        s/Str
                       (s/optional-key :inverse_rel) s/Str

                       (s/optional-key :attributes)  {s/Keyword s/Any}

                       :links                        {(s/optional-key :_collaboration_roots) [s/Str]
                                                      (s/optional-key :self)                 s/Str}
                       })


;; -- ENTITIES -- ;;

(s/defschema NewEntity {:type       s/Str
                        :attributes {s/Keyword s/Any}})

(s/defschema BaseEntity (assoc NewEntity :_rev s/Str
                                         :_id s/Uuid
                                         (s/optional-key :api_version) s/Int))


(s/defschema Entity (assoc BaseEntity
                      (s/optional-key :owner) s/Uuid
                      :links {:self                                  s/Str
                              (s/optional-key :tags)                 s/Str
                              (s/optional-key :properties)           s/Str
                              (s/optional-key :notes)                s/Str
                              (s/optional-key :timeline-events)      s/Str

                              s/Keyword                              {:self    s/Str
                                                                      :related s/Str}

                              (s/optional-key :_collaboration_roots) [s/Str]}))


(s/defschema EntityUpdate (dissoc BaseEntity :links))



;; -- Analyses -- ;;

(s/defschema NewAnalysisRecord
  {:inputs                      [s/Uuid]
   :outputs                     [s/Uuid]
   (s/optional-key :parameters) {s/Keyword s/Any}})


;; -- TRASH INFO -- ;;

(s/defschema TrashInfoMap {(keyword k/TRASHING-USER) s/Str  ;; URI
                           (keyword k/TRASHING-DATE) s/Str  ;; ISO DateTime
                           (keyword k/TRASH-ROOT)    s/Str  ;; URI
                           })

(s/defschema TrashedEntity (assoc Entity (s/optional-key :trash_info) TrashInfoMap))

;; -- Relationships -- ;;

(def EntityChildren
  {:project {:folder {:rel         "folders"
                      :inverse-rel "parents"}
             :file   {:rel         "files"
                      :inverse-rel "parents"}}

   :folder  {:folder {:rel         "folders"
                      :inverse-rel "parents"}
             :file   {:rel         "files"
                      :inverse-rel "parents"}}

   :source  {:source {:rel         "children"
                      :inverse-rel "parents"}}

   :file    {:revision {:rel         "revisions"
                        :inverse-rel "file"}}})

(def EntityRelationships
  {:project  {:folders {:schema Entity}
              :files   {:schema Entity}}

   :source   {:children {:schema Entity}
              :parents  {:schema Entity}
              :files    {:schema Entity}}

   :folder   {:children {:schema Entity}
              :parents  {:schema Entity}
              :files    {:schema Entity}}

   :file     {:revisions {:schema Entity}
              :parents   {:schema Entity}}

   :revision {:file {:schema Entity}}})


