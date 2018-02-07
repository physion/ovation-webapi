(ns ovation.db.relations
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "ovation/db/sql/relations.sql" {:quoting :mysql})

(hugsql/def-sqlvec-fns "ovation/db/sql/relations.sql" {:quoting :mysql})
