(ns ovation.db.files
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "ovation/db/sql/files.sql" {:quoting :mysql})

(hugsql/def-sqlvec-fns "ovation/db/sql/files.sql" {:quoting :mysql})
