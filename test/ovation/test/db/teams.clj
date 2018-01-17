(ns ovation.test.db.teams
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "ovation/test/db/sql/teams.sql" {:quoting :mysql})

(hugsql/def-sqlvec-fns "ovation/test/db/sql/teams.sql" {:quoting :mysql})
