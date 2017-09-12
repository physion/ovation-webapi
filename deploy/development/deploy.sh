#!/bin/bash

set -e

KUBERNETES_CLUSTER_NAME=ovation
KUBERNETES_APP_NAME=webapi
DEFAULT_ZONE=us-east1-b
PROJECT_ID=ovation-staging
WORKDIR=deploy/development
NAMESPACE=development

codeship_google authenticate

echo "Setting Project ID $PROJECT_ID"
gcloud config set project $PROJECT_ID

echo "Setting default timezone $DEFAULT_ZONE"
gcloud config set compute/zone $DEFAULT_ZONE

cd $WORKDIR

#sed -i "s/DATE/`date +'%s'`/" indexer.Deployment.yaml

echo "Applying deployment..."
kubectl --namespace=$NAMESPACE apply -f webapi.Deployment.yaml

echo "Applying service..."
kubectl --namespace=$NAMESPACE apply -f webapi.Service.yaml

IMAGE=gcr.io/ovation/webapi:development-$CI_TIMESTAMP
echo "Setting deployment image $IMAGE..."
kubectl --namespace=$NAMESPACE rolling-update webapi --image=$IMAGE
