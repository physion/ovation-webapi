(ns ovation-api-webservice.user-view)

(defn index-user [params]
  (let [
        status (if-not (contains? params :api-key)
                 (num 401)
                 (num 200)
               )
        body (if (= 200 status)
               (str "[{
    \"type\" : \"User\",
    \"access\" : \"read\",
    \"attributes\" : {
        \"_id\": \"14c1dd90-69b3-0131-a68a-12313d009d02\",
        \"_rev\": \"1-deb615394fbb0ed296a32a1492da788f\",
        \"username\": \"Alice Demo\",
        \"email\": \"alice_demo@physion.us\",
    },
    \"named_links\" : {
        \"attachments\" : {}
    }
}]")
               (str "Please log in to get your users")
             )
       ]
    {:status status
     :body body}
  )
)

(defn get-user [params]
  (let [
    status (if (or (not (contains? params :api-key)) (not (contains? params :id)))
             (num 401)
             (num 200)
           )
    body (if (= 200 status)
           (str "[{
    \"type\" : \"User\",
    \"access\" : \"read\",
    \"attributes\" : {
        \"_id\": \"PUT-ID-HERE\",
        \"_rev\": \"1-deb615394fbb0ed296a32a1492da788f\",
        \"username\": \"Alice Demo\",
        \"email\": \"alice_demo@physion.us\",
    },
    \"named_links\" : {
        \"attachments\" : {}
    }
}]")
           (str "Please log in to get your users")
         )
       ]
    {:status status
     :body body}
  )
)

(defn create-user [params]
  "TODO"
)

(defn update-user [params]
  "TODO"
)

(defn delete-user [params]
  "TODO"
)


