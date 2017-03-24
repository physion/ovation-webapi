(ns ovation.request-context
  (:require [ovation.auth :as auth]
            [compojure.api.routes :refer [path-for*]]))

(defprotocol AuthToken
  (token [this])
  (team-ids [this])
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

(defn router
  [request]
  (fn [name & [params]]
    (path-for* name request params)))

(defn make-context
  "Constructs a RequestContext from a request"
  [request org]
  (map->RequestContext {::org     org
                        ::routes  (router request)
                        ::auth    (auth/identity request)
                        ::request request}))
