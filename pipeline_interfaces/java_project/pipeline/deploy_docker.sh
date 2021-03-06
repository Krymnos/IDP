#!/bin/bash
docker login -u="$DOCKER_USERNAME" -p="$DOCKER_PASSWORD"
docker build -t pipeline_java .
docker images
docker tag pipeline_java cloudproto/pipeline_component
docker push cloudproto/pipeline_component