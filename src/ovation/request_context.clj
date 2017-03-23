(ns ovation.request-context
  (:require [ovation.routes :as routes]
            [ovation.auth :as auth]))

(defprotocol AuthToken
  (token [this])
  (team-ids [thix])
  (user-id [this]))

(defrecord RequestContext
  [auth org routes]

  AuthToken
  (token [c]
    (get-in c [::auth ::auth/token]))
  (team-ids [c]
    (auth/authenticated-teams (::auth c)))
  (user-id [c]
    (auth/authenticated-user-id (::auth c))))

(defn make-context
  "Constructs a RequestContext from a request"
  [request org]
  (map->RequestContext {::org     org
                        ::routes  (routes/router request)
                        ::auth    (auth/identity request)
                        ::request request}))
