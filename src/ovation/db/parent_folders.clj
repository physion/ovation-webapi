(ns ovation.db.parent_folders
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "ovation/db/sql/parent_folders.sql" {:quoting :mysql})

(hugsql/def-sqlvec-fns "ovation/db/sql/parent_folders.sql" {:quoting :mysql})
