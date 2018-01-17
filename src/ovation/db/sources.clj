(ns ovation.db.sources
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "ovation/db/sql/sources.sql" {:quoting :mysql})

(hugsql/def-sqlvec-fns "ovation/db/sql/sources.sql" {:quoting :mysql})
