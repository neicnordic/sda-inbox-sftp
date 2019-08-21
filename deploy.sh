#!/bin/bash

if [[ "$TRAVIS_BRANCH" == "master" && "$TRAVIS_PULL_REQUEST" == "false" ]]; then
  curl $TRYGGVE_CA --create-dirs -o ~/.docker/ca.pem
  curl $TRYGGVE_CERT --create-dirs -o ~/.docker/cert.pem
  curl $TRYGGVE_KEY --create-dirs -o ~/.docker/key.pem
  docker stack rm INBOX
  docker config rm CA.cert
  docker config rm inbox.jks
  docker config create CA.cert CA.cert
  docker config create inbox.jks inbox.jks
  export S3_ACCESS_KEY=$TEMP_S3_ACCESS_KEY
  export S3_ENDPOINT=$TEMP_S3_ENDPOINT
  export S3_SECRET_KEY=$TEMP_S3_SECRET_KEY
  docker build --no-cache -t nbisweden/ega-mina-inbox:uh .
  docker stack deploy INBOX --compose-file docker-stack.yml
fi
