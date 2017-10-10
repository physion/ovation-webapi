#!/bin/bash

set -e

NAMESPACE=development

kubectl create secret generic development-webapi-service-key \
 --namespace=$NAMESPACE \
 --from-file=key.json=<PATH-TO-KEY-FILE>.json
