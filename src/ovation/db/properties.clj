(ns ovation.db.properties
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "ovation/db/sql/properties.sql" {:quoting :mysql})

(hugsql/def-sqlvec-fns "ovation/db/sql/properties.sql" {:quoting :mysql})
