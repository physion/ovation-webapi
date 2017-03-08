(ns ovation.request-context
  (:require [ovation.routes :as routes]
            [ovation.auth :as auth]))

(defrecord RequestContext
  [auth org routes])

(defn make-context
  "Constructs a RequestContext from a request"
  [request org]
  #RequestContext{:org     org
                  :routes  (routes/router request)
                  :auth    (auth/identity request)
                  :request request})
