(ns ovation-api-webservice.view)

(defn index-project [params]
  (let [
        status (if-not (contains? params :api-key)
                 (num 401)
                 (num 200)
                 )
        body (if (= 200 status)
               (str "[{ \"type\" : \"Project\",
  \"access\" : \"write\",
  \"_id\": \"bb1fda63-ee8a-46af-b986-958d924bfbcf\",
  \"_rev\": \"4-2185cd00eaf3c5f002626fc1713dc563\",
  \"name\": \"Alice's project\",
  \"attributes\" : {
      \"name\": \"Alice's project\",
      \"purpose\": \"qPCR experiments and analysis\",
      \"start\": \"2014-01-29T02:21:24.236-05:00\",
      \"startZone\": \"America/New_York\"
  },
  
  \"links\" : {
      \"experiments\" : {
          \"href\" : \"/api/v1/entities/bb1fda63-ee8a-46af-b986-958d924bfbcf/experiments\",
          \"count\" : 0
      },
      \"projects\" : {
          \"href\" : \"/api/v1/entities/bb1fda63-ee8a-46af-b986-958d924bfbcf/projects\",
          \"count\" : 1
      },
      \"owner\" : {
          \"href\" : \"/api/v1/entities/14c1dd90-69b3-0131-a68a-12313d009d02\",
          \"count\" : 1
      },
      \"analysis_records\" : {
          \"href\" : \"/api/v1/entities/{attributes._id}/analyses\",
          \"count\" : 0
      }
  }
}]"
               )
               (str "Please log in to get your Projects")
             )
       ]
    {:status status
     :body body}
  )
)
