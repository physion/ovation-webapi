(ns ovation.db.activities
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "ovation/db/sql/activities.sql" {:quoting :mysql})

(hugsql/def-sqlvec-fns "ovation/db/sql/activities.sql" {:quoting :mysql})
