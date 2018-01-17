(ns ovation.db.uuids
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "ovation/db/sql/uuids.sql" {:quoting :mysql})

(hugsql/def-sqlvec-fns "ovation/db/sql/uuids.sql" {:quoting :mysql})
