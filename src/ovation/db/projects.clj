(ns ovation.db.projects
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "ovation/db/sql/projects.sql" {:quoting :mysql})

(hugsql/def-sqlvec-fns "ovation/db/sql/projects.sql" {:quoting :mysql})
