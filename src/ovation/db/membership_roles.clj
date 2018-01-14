(ns ovation.db.membership_roles
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "ovation/db/sql/membership_roles.sql" {:quoting :mysql})

(hugsql/def-sqlvec-fns "ovation/db/sql/membership_roles.sql" {:quoting :mysql})
