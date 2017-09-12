#!/bin/bash

set -e

source dev.gke.env

NAMESPACE=development
KUBERNETES_CLUSTER_NAME=ovation
KUBERNETES_APP_NAME=webapi
DEFAULT_ZONE=us-east1-b
PROJECT_ID=ovation-staging
WORKDIR=deploy/development
NAMESPACE=development


kubectl create configmap development-webapi-config \
 --namespace=$NAMESPACE \
 --from-literal=CLOUDANT_DB_URL=$CLOUDANT_DB_URL \
 --from-literal=OVATION_IO_HOST_URI=$OVATION_IO_HOST_URI \
 --from-literal=GOOGLE_CLOUD_PROJECT_ID=$GOOGLE_CLOUD_PROJECT_ID
