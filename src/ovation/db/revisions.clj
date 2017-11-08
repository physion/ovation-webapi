(ns ovation.db.revisions
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "ovation/db/sql/revisions.sql" {:quoting :mysql})

(hugsql/def-sqlvec-fns "ovation/db/sql/revisions.sql" {:quoting :mysql})
