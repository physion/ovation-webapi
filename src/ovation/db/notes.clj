(ns ovation.db.notes
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "ovation/db/sql/notes.sql" {:quoting :mysql})

(hugsql/def-sqlvec-fns "ovation/db/sql/notes.sql" {:quoting :mysql})
