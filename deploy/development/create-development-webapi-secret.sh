#!/bin/bash

set -e

source dev.gke.env


NAMESPACE=development

kubectl create secret generic development-webapi-secret \
 --namespace=$NAMESPACE \
 --from-literal=CLOUDANT_PASSWORD=$CLOUDANT_PASSWORD \
 --from-literal=CLOUDANT_USERNAME=$CLOUDANT_USERNAME \
 --from-literal=JWT_SECRET=$JWT_SECRET \
 --from-literal=RAYGUN_API_KEY=$RAYGUN_API_KEY
