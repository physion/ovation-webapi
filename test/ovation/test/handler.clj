(ns ovation.test.handler
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [ovation.handler :as handler]
            [clojure.data.json :as json]
            [slingshot.slingshot :refer [try+]]
            [ovation.auth :as auth]
            [ovation.core :as core]
            [clojure.walk :as walk]
            [ovation.util :as util])
  (:import (java.util UUID)))

(defn mock-req
  [req apikey]
  (-> req
    (mock/header "Authorization" (str "Token token=" apikey))
    (mock/content-type "application/json")))

(facts "About authorization"
  (fact "invalid API key returns 401"
    (let [apikey "12345"
          get (mock-req (mock/request :get "/api/v1/entities/123") apikey)
          auth-response (promise)
          _ (deliver auth-response {:status 401})]
      (:status (handler/app get)) => 401
      (provided
        (auth/get-auth anything apikey) => auth-response))))


(facts "About doc route"
  (let [apikey "..apikey.."
        auth-response (promise)
        _ (deliver auth-response {:status 200 :body "{\"user\": \"..user..\"}"})]
    (against-background [(auth/get-auth anything apikey) => auth-response]
      (fact "HEAD / => 302"
        (let [response (handler/app (mock-req (mock/request :head "/") apikey))]
          (:status response) => 302))
      (fact "GET / redirects"
        (let [response (handler/app (mock-req (mock/request :get "/") apikey))]
          (:status response) => 302
          (-> response (:headers) (get "Location")) => "/index.html"))
      (fact "HEAD /index.html returns 200"
        (let [response (handler/app (mock-req (mock/request :head "/index.html") apikey))]
          (:status response) => 200))))
  )

(facts "About invalid routes"
  (let [apikey "..apikey.."
        auth-response (promise)
        _ (deliver auth-response {:status 200 :body "{\"user\": \"..user..\"}"})]
    (against-background [(auth/get-auth anything apikey) => auth-response]
      (fact "invalid path =>  404"
        (let [response (handler/app (mock-req (mock/request :get "/invalid/path") apikey))]
          response => nil?)))))

(facts "About /entities"
  (let [apikey "..apikey.."
        auth-response (promise)
        auth-info {:user "..user.."}
        _ (deliver auth-response {:status 200 :body (util/to-json auth-info)})]
    (against-background [(auth/get-auth anything apikey) => auth-response]
      (facts "read"
        (let [id (str (UUID/randomUUID))
              get (mock-req (mock/request :get (str "/api/v1/entities/" id)) apikey)
              doc {:_id        id
                   :_rev       "123"
                   :type       "Entity"
                   :links      {}
                   :attributes {}}]

          (against-background [(core/get-entities auth-info [id]) => [doc]]
            (fact "GET /entities/:id returns status 200"
              (:status (handler/app get)) => 200)
            (fact "GET /entities/:id returns doc"
              (walk/keywordize-keys (json/read (clojure.java.io/reader (:body (handler/app get))))) => {:entity doc})))))))
;
;
;(facts "About entities"
;       (fact "POST /entities inserts an entity (201)"
;             (let [dto {:type       "Project"
;                        :attributes {}}
;                   post (-> (mock/request :post "/api/v1/entities")
;                            (mock/query-string {"api-key" "12345"})
;                            (mock/content-type "application/json")
;                            (mock/body (json/write-str (walk/stringify-keys dto))))]
;               (:status (handler/app post)) => 201
;               (provided
;                 (entity/create-entity "12345" {:type       "Project"
;                                                :attributes {}}) => [{:_id        "id"
;                                                                      :_rev       "rev"
;                                                                      :attributes {}
;                                                                      :type       "Project"}])))
;
;       (fact "PUT /entities/:id updates the attributes in entity (200)"
;             (let [uuid (UUID/randomUUID)
;                   id (str uuid)
;                   dto {:type       "Project"
;                        :attributes {"attr1" 1
;                                     "attr2" "value"}
;                        :_id        id
;                        :_rev       "rev"}]
;
;               (:status (handler/app (-> (mock/request :put "/api/v1/entities/123")
;                                         (mock/query-string {"api-key" "12345"})
;                                         (mock/content-type "application/json")
;                                         (mock/body (json/write-str (walk/stringify-keys dto)))))) => 200
;               (provided
;                 (entity/update-entity-attributes "12345" "123" {:attr1 1
;                                                                 :attr2 "value"}) => [{:_id        (str uuid)
;                                                                                       :_rev       "rev2"
;                                                                                       :attributes {"attr1" 1}
;                                                                                       :links      {}
;                                                                                       :type       "Project"}])))
;
;       (fact "POST /entities/:id/links creates a new link (201)"
;             (let [target_uuid (UUID/randomUUID)
;                   project_uuid (UUID/randomUUID)
;                   project_id (str project_uuid)
;                   target_id (str target_uuid)
;                   link {:target_id   target_id
;                         :rel         "patients"
;                         :inverse_rel "projects"}
;                   body (json/write-str (walk/stringify-keys link))
;                   path (str "/api/v1/entities/" project_id "/links")
;                   apikey "12345"]
;
;               (:status (handler/app (-> (mock/request :post path)
;                                         (mock/query-string {"api-key" apikey})
;                                         (mock/content-type "application/json")
;                                         (mock/body body)))) => 201
;               (provided
;                 (links/create-link apikey project_id {:target_id   target_uuid
;                                                       :rel         "patients"
;                                                       :inverse_rel "projects"}) => {:success true})))
;
;       (fact "POST /entities/:id/links/:rel creates new a link (201)"
;             (let [target_uuid (UUID/randomUUID)
;                   entity_id (str (UUID/randomUUID))
;                   inverse_rel "myrel-inverse"
;                   link {:target_id   (str target_uuid)
;                         :inverse_rel inverse_rel}
;                   rel "myrel"
;                   apikey "12345"
;                   post (-> (mock/request :post (str "/api/v1/entities/" entity_id "/links/" rel))
;                            (mock/query-string {"api-key" apikey})
;                            (mock/content-type "application/json")
;                            (mock/body (json/write-str (walk/stringify-keys link))))]
;
;               (:status (handler/app post)) => 201
;
;               (provided
;                 (links/create-link apikey entity_id {:target_id   (str target_uuid)
;                                                      :rel         rel
;                                                      :inverse_rel inverse_rel}) => {:success true})))
;
;       (fact "POST /entities/:id/named_links/:rel/:named creates new a link (201)"
;             (let [target_uuid (str (UUID/randomUUID))
;                   entity_id (str (UUID/randomUUID))
;                   inverse_rel "myrel-inverse"
;                   name "myrelname"
;                   link {:target_id   target_uuid
;                         :inverse_rel inverse_rel}
;                   rel "myrel"
;                   apikey "12345"
;                   post (-> (mock/request :post (str "/api/v1/entities/" entity_id "/named_links/" rel "/" name))
;                            (mock/query-string {"api-key" apikey})
;                            (mock/content-type "application/json")
;                            (mock/body (json/write-str (walk/stringify-keys link))))]
;
;               (:status (handler/app post)) => 201
;
;               (provided
;                 (links/create-named-link apikey entity_id {:target_id   target_uuid
;                                                            :rel         rel
;                                                            :inverse_rel inverse_rel
;                                                            :name        name}) => {:success true})))
;       )
;
