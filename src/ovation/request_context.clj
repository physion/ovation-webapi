(ns ovation.request-context
  (:require [ovation.auth :as auth]
            [compojure.api.routes :refer [path-for*]]
            [clojure.walk :as walk]))

(defprotocol AuthToken
  (token [this])
  (team-ids [this])
  (user-id [this])
  (organization-ids [this]))

(defrecord RequestContext
  [auth org routes query-params]

  AuthToken
  (token [c]
    (get-in c [::auth ::auth/token]))
  (team-ids [c]
    (auth/authenticated-teams (::auth c)))
  (user-id [c]
    (auth/authenticated-user-id (::auth c)))
  (organization-ids [c]
    (auth/organization-ids (::request c))))

(defn router
  [request]
  (fn [name & [params]]
    (path-for* name request params)))

(defn make-context
  "Constructs a RequestContext from a request"
  [request org]
  (map->RequestContext {::org          org
                        ::routes       (router request)
                        ::auth         (auth/identity request)
                        ::request      request
                        ::query-params (walk/keywordize-keys (:query-params request))}))
