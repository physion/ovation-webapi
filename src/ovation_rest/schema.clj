(ns ovation-rest.schema
  (:require [schema.core :as s]))

;;; --- Schema Definitions --- ;;;

(s/defschema Success {:success s/Bool})

(s/defschema Entity {:type                         s/Str    ;(s/enum :Project :Protocol :User :Source)
                     :_rev                         s/Str
                     :_id                          s/Str    ; could we use s/uuid here?
                     :links                        {s/Keyword s/Str}
                     :attributes                   {s/Keyword s/Str}
                     (s/optional-key :named_links) {s/Keyword {s/Keyword s/Str}}
                     (s/optional-key :annotations) s/Any
                     })

(s/defschema NewEntity (assoc (dissoc Entity :_id :_rev :links) (s/optional-key :links) {s/Keyword [s/Str]}))


(s/defschema NewLink {:type                         us.physion.ovation.values.Relation/RELATION_TYPE
                      :target_id                    s/Uuid
                      (s/optional-key :inverse_rel) s/Str
                      })

(s/defschema NewNamedLink (assoc NewLink :name s/Str))

(s/defschema Link (assoc NewLink :_id s/Str
                                 :_rev s/Str
                                 :source_id s/Str           ;; URI
                                 :target_id s/Str           ;; URI
                                 :rel s/Str
                                 (s/optional-key :name) s/Str
                                 (s/optional-key :inverse_rel) s/Str
                                 :links {:_collaboration_roots [s/Str]}))
