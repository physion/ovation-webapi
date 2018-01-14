(ns ovation.test.db.organizations
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "ovation/test/db/sql/organizations.sql" {:quoting :mysql})

(hugsql/def-sqlvec-fns "ovation/test/db/sql/organizations.sql" {:quoting :mysql})
