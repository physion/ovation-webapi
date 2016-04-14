#!/bin/bash

CATALYZE_CLI=catalyze_3.1.5_linux_amd64
CATALYZE_VERISON=3.1.5

if [ ! -d "$CATALYZE_CLI" ]; then
    # Install catalyze-cli
    wget https://github.com/catalyzeio/cli/releases/download/$CATALYZE_VERSION/$CATALYZE_CLI.tar.gz
    tar xzvf $CATALYZE_CLI.tar.gz
fi


# Associate catalyze environment
$CATALYZE_CLI/catalyze  --username=$CATALYZE_USER  --password=$CATALYZE_PASSWORD associate $CATALYZE_ENVIRONMENT app

# Deploy
git fetch --unshallow || true
git push catalyze ${CI_COMMIT_ID}:master
