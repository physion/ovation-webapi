(ns user
  (:require [ovation.system :as system]
            [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            ;; dev-system.clj only contains: (def the-system)
            [ovation.dev-system :refer [the-system]])

  (def system-config
       {:web-server {:port 3000
                     :host "localhost"}
        :db {:host 3456
             :host "localhost"}
        :handler {cookie-config {}}}


       (defn init []
         (alter-root-var #'the-system
           (constantly system/create-system system-config)))

       (defn start []
         (alter-root-var #'the-system component/start))

       (defn stop []
         (alter-root-var #'the-system
           #(when % (component/stop %))))

       (defn go []
         (init)
         (start))

       (defn reset []
         (stop)
         (refresh :after 'user/go))
