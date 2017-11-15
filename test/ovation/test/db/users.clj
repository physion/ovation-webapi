(ns ovation.test.db.users
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "ovation/test/db/sql/users.sql" {:quoting :mysql})

(hugsql/def-sqlvec-fns "ovation/test/db/sql/users.sql" {:quoting :mysql})
