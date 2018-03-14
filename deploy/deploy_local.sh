#!/bin/bash

NAMESPACE=$1

set -e

RELEASE_NAME=webapi-$NAMESPACE
DEFAULT_ZONE=us-east1-b
KUBERNETES_CLUSTER_NAME=ovation

echo "Upgrading webapi release..."

echo "NAMESPACE = $NAMESPACE"
echo "RELEASE_NAME = $RELEASE_NAME"
echo "CI_TIMESTAMP = $CI_TIMESTAMP"

# Update dependencies
helm repo add cos https://centerforopenscience.github.io/helm-charts/
helm dependencies update ./deploy/ovation-webapi/


helm-wrapper upgrade --install --debug --namespace=${NAMESPACE} \
    --set image.tag=${NAMESPACE}-${CI_TIMESTAMP} \
    -f ./deploy/values/${NAMESPACE}/secrets.yaml \
    ${RELEASE_NAME} \
    ./deploy/ovation-webapi/
