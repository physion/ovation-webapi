(ns ovation-rest.schema
  (:require [schema.core :as s]))

;;; --- Schema Definitions --- ;;;

(s/defschema Success {:success s/Bool})

(s/defschema Link {:type                         us.physion.ovation.values.Relation/RELATION_TYPE
                   :target_id                    s/Uuid
                   :rel                          s/Str
                   (s/optional-key :inverse_rel) s/Str})

(s/defschema NamedLink (assoc Link :name s/Str))


(s/defschema NewEntityLink {:target_id                    s/Uuid
                            (s/optional-key :inverse_rel) s/Str})

(s/defschema Entity {:type                         s/Str    ;(s/enum :Project :Protocol :User :Source)
                     :_rev                         s/Str
                     :_id                          s/Uuid   ; could we use s/uuid here?
                     :attributes                   {s/Keyword (s/either s/Num s/Str)}
                     (s/optional-key :links)       {s/Keyword s/Str}
                     (s/optional-key :named_links) {s/Keyword {s/Keyword s/Str}}
                     (s/optional-key :annotations) s/Any})

(s/defschema NewEntity (assoc (dissoc Entity :_id :_rev :links :named_links)
                         (s/optional-key :links) {s/Keyword [NewEntityLink]}
                         (s/optional-key :named_links) {s/Keyword {s/Keyword [NewEntityLink]}}))



