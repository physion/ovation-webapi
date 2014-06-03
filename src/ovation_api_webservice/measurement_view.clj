(ns ovation-api-webservice.measurement-view)

(defn index-measurement [params]
  (let [
        status (if-not (contains? params :api-key)
                 (num 401)
                 (num 200)
               )
        body (if (= 200 status)
               (str "[{
    \"type\" : \"Measurement\",
    \"access\" : \"read|write\",
    \"_id\" : \"dd3e7e9a-5bd9-4045-aca5-ff6d280ede4d\",
    \"_rev\": \"1-2527da57957c70d4285f9a1333e61fb4\",
    \"attributes\" : {
        \"name\": \"A7_HYU85_corrected\",
        \"devices\" : [\"QuantStudio\"],
    },
    \"links\" : {
        \"owner\" : {
            \"href\" : \"/api/v1/entities/14c1dd90-69b3-0131-a68a-12313d009d02\",
            \"count\" : 1
        },
        \"experiments\" : {
            \"href\" : \"/api/v1/entities/dd3e7e9a-5bd9-4045-aca5-ff6d280ede4d/experiments\",
            \"count\" : 0
        },
        \"projects\" : {
            \"href\" : \"/api/v1/entities/dd3e7e9a-5bd9-4045-aca5-ff6d280ede4d/projects\",
            \"count\" : 1
        },
        \"attachments\" : {
            \"href\" : \"/api/v1/entities/dd3e7e9a-5bd9-4045-aca5-ff6d280ede4d/attachments\",
            \"count\" : 0
        },
        \"epoch\" : {
            \"href\" : \"/api/v1/entities/6f7cca39-3158-4998-bae6-3edd074b77dc\",
            \"count\" : 1
        },
        \"sources\" : {
            \"Naprox_11-0030\" : {
                \"href\" : \"/api/v1/entities/a1b862e3-ccf5-4bc1-a2b4-7ca5ce20fb65\",
                \"count\" : 1
            },
            \"Naprox_11-0002\" : {
                \"href\" : \"/api/v1/entities/0f9ee7e9-3a7b-41fa-922b-c4dded245f36\",
                \"count\" : 1
            }
        },
        /* Option 1 */
        \"data_resource\" : {
            \"href\" : \"/api/v1/entities/d2bae4ca-1fc1-4648-968e-0399bb682cc1\",
            \"count\" : 1
        },
        /* Option 2 */
        \"data_resource\" : {
            \"href\" : \"https://ovation.io/api/v1/resources/55067\",
            \"content-type\" : \"application/x-quantstudio-eds\",
            \"filename\" : \"A7_HYU85_corrected.eds\",
            \"supporting_files\" : [
                { 
                    \"href\" : \"https://ovation.io/api/v1/resources/xxx\",
                    \"content-type\" : \"application/x-other\",
                    \"filename\" : \"path/to/other/file.ext\"
                }
            ]
        },
        \"analysis_records\" : {
            \"href\" : \"/api/v1/entities/{attributes._id}/analyses\",
            \"count\" : 0
        }
    }
}]"
               )
               (str "Please log in to get your measurements")
             )
       ]
    {:status status
     :body body}
  )
)

(defn get-measurement [params]
  (let [
    status (if (or (not (contains? params :api-key)) (not (contains? params :id)))
             (num 401)
             (num 200)
           )
    body (if (= 200 status)
           (str "[{
    \"type\" : \"Measurement\",
    \"access\" : \"read|write\",
    \"_id\" : \"PUT-ID-HERE\",
    \"_rev\": \"1-2527da57957c70d4285f9a1333e61fb4\",
    \"attributes\" : {
        \"name\": \"A7_HYU85_corrected\",
        \"devices\" : [\"QuantStudio\"],
    },
    \"links\" : {
        \"owner\" : {
            \"href\" : \"/api/v1/entities/14c1dd90-69b3-0131-a68a-12313d009d02\",
            \"count\" : 1
        },
        \"experiments\" : {
            \"href\" : \"/api/v1/entities/dd3e7e9a-5bd9-4045-aca5-ff6d280ede4d/experiments\",
            \"count\" : 0
        },
        \"projects\" : {
            \"href\" : \"/api/v1/entities/dd3e7e9a-5bd9-4045-aca5-ff6d280ede4d/projects\",
            \"count\" : 1
        },
        \"attachments\" : {
            \"href\" : \"/api/v1/entities/dd3e7e9a-5bd9-4045-aca5-ff6d280ede4d/attachments\",
            \"count\" : 0
        },
        \"epoch\" : {
            \"href\" : \"/api/v1/entities/6f7cca39-3158-4998-bae6-3edd074b77dc\",
            \"count\" : 1
        },
        \"sources\" : {
            \"Naprox_11-0030\" : {
                \"href\" : \"/api/v1/entities/a1b862e3-ccf5-4bc1-a2b4-7ca5ce20fb65\",
                \"count\" : 1
            },
            \"Naprox_11-0002\" : {
                \"href\" : \"/api/v1/entities/0f9ee7e9-3a7b-41fa-922b-c4dded245f36\",
                \"count\" : 1
            }
        },
        /* Option 1 */
        \"data_resource\" : {
            \"href\" : \"/api/v1/entities/d2bae4ca-1fc1-4648-968e-0399bb682cc1\",
            \"count\" : 1
        },
        /* Option 2 */
        \"data_resource\" : {
            \"href\" : \"https://ovation.io/api/v1/resources/55067\",
            \"content-type\" : \"application/x-quantstudio-eds\",
            \"filename\" : \"A7_HYU85_corrected.eds\",
            \"supporting_files\" : [
                { 
                    \"href\" : \"https://ovation.io/api/v1/resources/xxx\",
                    \"content-type\" : \"application/x-other\",
                    \"filename\" : \"path/to/other/file.ext\"
                }
            ]
        },
        \"analysis_records\" : {
            \"href\" : \"/api/v1/entities/{attributes._id}/analyses\",
            \"count\" : 0
        }
    }
}]"
               )
           (str "Please log in to get your measurements")
         )
       ]
    {:status status
     :body body}
  )
)

(defn create-measurement [params]
  "TODO"
)

(defn update-measurement [params]
  "TODO"
)

(defn delete-measurement [params]
  "TODO"
)


