(ns ovation.request-context
  (:require [ovation.routes :as routes]
            [ovation.auth :as auth]))

(defprotocol AuthToken
  (token [c])
  (team-ids [c])
  (user-id [c]))

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
  #RequestContext{::org     org
                  ::routes  (routes/router request)
                  ::auth    (auth/identity request)
                  ::request request})
