(ns ovation.db.folders
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "ovation/db/sql/folders.sql" {:quoting :mysql})

(hugsql/def-sqlvec-fns "ovation/db/sql/folders.sql" {:quoting :mysql})
