(ns ovation.db.tags
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "ovation/db/sql/tags.sql" {:quoting :mysql})

(hugsql/def-sqlvec-fns "ovation/db/sql/tags.sql" {:quoting :mysql})
