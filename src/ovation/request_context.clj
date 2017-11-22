(ns ovation.request-context
  (:require [ovation.auth :as auth]
            [compojure.api.routes :refer [path-for*]]
            [clojure.walk :as walk]
            [ovation.authz :as authz]))

(defprotocol AuthToken
  (token [this])
  (team-ids [this])
  (user-id [this])
  (organization-ids [this])
  (authorization [this])
  (authorization-ch [this]))

(defn router
  [request]
  (fn [name & [params]]
    (path-for* name request params)))

(defrecord RequestContext [authz]

  AuthToken
  (token [c]
    (get-in c [::identity ::auth/token]))
  (team-ids [c]
    (auth/authenticated-teams (::identity c)))
  (user-id [c]
    (auth/authenticated-user-uuid (::identity c)))
  (organization-ids [c]
    (auth/organization-ids (::request c)))
  (authorization-ch [c]
    (if authz
      (authz/get-authorization-ch authz c)
      nil))
  (authorization [c]
    (if authz
      (authz/get-authorization authz c)
      nil)))

(defn make-context
  "Constructs a RequestContext from a request"
  [request org authz]
  (map->RequestContext {:authz         authz
                        ::org          org
                        ::routes       (router request)
                        ::identity     (auth/identity request)
                        ::request      request
                        ::query-params (walk/keywordize-keys (:query-params request))}))
