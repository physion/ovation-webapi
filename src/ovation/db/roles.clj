(ns ovation.db.roles
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "ovation/db/sql/roles.sql" {:quoting :mysql})

(hugsql/def-sqlvec-fns "ovation/db/sql/roles.sql" {:quoting :mysql})
