(ns ovation.db.memberships
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "ovation/db/sql/memberships.sql" {:quoting :mysql})

(hugsql/def-sqlvec-fns "ovation/db/sql/memberships.sql" {:quoting :mysql})
