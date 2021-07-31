#!/usr/bin/env bash

# this assumes DOCKER_PASSWORD and DOCKER_USERNAME are set
# externally
#
# also assumes the jobson-docker module was built, which loads
# the local client with the `adamkewley/jobson:TAG` version

if [[ -z "${DOCKER_USERNAME}" ]]; then
    echo "DOCKER_USERNAME environment variable is not set: this is required for deployment"
    exit 1
fi

if [[ -z "${DOCKER_PASSWORD}" ]]; then
    echo "DOCKER_PASSWORD environment variable is not set: this is required for deployment"
    exit 1
fi

if [[ -z "${JOBSON_TAG}" ]]; then
    echo "JOBSON_TAG environment variable is not set: this is required for deployment"
    exit 1
fi

docker login --username "$DOCKER_USERNAME" --password "$DOCKER_PASSWORD"
docker push ${DOCKER_USERNAME}/jobson:${JOBSON_TAG}
docker push ${DOCKER_USERNAME}/jobson:latest
