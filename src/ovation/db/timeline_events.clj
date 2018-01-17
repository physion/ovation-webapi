(ns ovation.db.timeline_events
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "ovation/db/sql/timeline_events.sql" {:quoting :mysql})

(hugsql/def-sqlvec-fns "ovation/db/sql/timeline_events.sql" {:quoting :mysql})
