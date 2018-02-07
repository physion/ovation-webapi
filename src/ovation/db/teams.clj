(ns ovation.db.teams
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "ovation/db/sql/teams.sql" {:quoting :mysql})

(hugsql/def-sqlvec-fns "ovation/db/sql/teams.sql" {:quoting :mysql})
