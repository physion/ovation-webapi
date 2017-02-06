(ns ovation.logging
  (:require [potemkin :refer [import-vars]]))

(import-vars
  [clojure.tools.logging
   log debug info warn error fatal])
