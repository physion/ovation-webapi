#!/bin/bash

ENVIRONMENT=$1

curl -L -O https://github.com/technomancy/leiningen/raw/2.4.3/bin/lein
chmod u+x lein
./lein with-profile ci do clean, beanstalk deploy $ENVIRONMENT
