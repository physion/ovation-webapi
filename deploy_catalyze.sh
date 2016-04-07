#!/bin/bash

# TODO install catalyze-cli
wget --quiet --output-document=- https://github.com/catalyzeio/cli/releases/download/3.1.5/catalyze_3.1.5_amd64.deb | dpkg --install -

# Associate catalyze environment
catalyze associate $CATALYZE_ENVIRONMENT app --username $CATALYZE_USER  --password $CATALYZE_PASSWORD

git push catalyze master
