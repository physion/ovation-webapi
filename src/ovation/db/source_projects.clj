(ns ovation.db.source_projects
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "ovation/db/sql/source_projects.sql" {:quoting :mysql})

(hugsql/def-sqlvec-fns "ovation/db/sql/source_projects.sql" {:quoting :mysql})
