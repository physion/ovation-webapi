#!/usr/bin/env bash

TOKEN=$1 # API token
PROJECT_ID=$2 # Project ID

### Get a single Project by ID
curl -X GET --header 'Accept: application/json'
            --header "Authorization: Bearer $TOKEN"
            'https://api.ovation.io/api/v1/projects/$PROJECT_ID'


## To upload a file, we need to create a File, then upload a Revision

### Create a new File belonging to the Project by POSTing to the Project resource. You must supply the file name.
curl -X POST --header 'Content-Type: application/json'
             --header 'Accept: application/json'
             --header "Authorization: Bearer $TOKEN"
             -d '{
                  "entities": [
                    {
                      "type": "File",
                      "attributes": {"name": "example.vcf"}
                    }
                  ]
                }' "https://api.ovation.io/api/v1/projects/$PROJECT_ID"


### Create a new Revision by POSTing to the File resource. You must supply the content (MIME) type of the revision as well as a name. The return contains a pre-signed URL where you can POST the file contents
curl -X POST --header 'Content-Type: application/json'
             --header 'Accept: application/json'
             --header "Authorization: Bearer $TOKEN"
             -d '{
                  "entities": [
                    {
                      "type": "Revision",
                      "attributes": {
                        "content_type": "application/x-vcf",
                        "name": "example.vcf"
                      }
                    }
                  ]
                }' 'https://api.ovation.io/api/v1/files/afad9a02-7ad5-4793-ad4e-2a200197b334'


### The response contains an "aws" attribute like
 "aws": [
    {
      "id": "b3e6a5d4-8485-4d50-9fbe-33173b13cce3",
      "aws": {
        "access_key_id": "ACCESS_KEY",
        "secret_access_key": "SECRET_KEY",
        "session_token": "SESSION_TOKEN",
        "expiration": "2016-05-17T03:41:02.000Z",
        "key": "5162ef4f-8c57-4c2c-8e35-2a3f4f114275/b3e6a5d4-8485-4d50-9fbe-33173b13cce3/example.vcf",
        "bucket": "users.ovation.io"
      }
    }


### Post the file contents using the AWS CLI (http://aws.amazon.com/cli/) and the access, secret and session tokens from the Revision response
AWS_ACCESS_KEY_ID=$ACCESS_KEY # Use ACCESS_KEY from Revision response
AWS_SECRET_ACCESS_KEY=$SECRET_KEY # Use SECRET_KEY from Revision response
AWS_SESSION_TOKEN=$SESSION_TOKEN # Use SESSION_TOKEN from Revision response
BUCKET='users.ovation.io' # Use BUCKET from Revision response
OBJ_KEY='5162ef4f-8c57-4c2c-8e35-2a3f4f114275/b3e6a5d4-8485-4d50-9fbe-33173b13cce3/example.vcf' # Use key from Revision response
aws s3 cp /local/path/to/example.vcf s3://$BUCKET/$OBJ_KEY

