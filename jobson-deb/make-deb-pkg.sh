#!/bin/bash

set -e

VERSION=$1

cd target

fpm \
    --force \
    \
    --name jobson \
    --version $VERSION \
    --maintainer "Adam Kewley <contact@adamkewley.com>" \
    --url "https://github.com/adamkewley/jobson" \
    --description "A web server that can turn command-line applications into a job system." \
    --license "Apache-2.0" \
    \
    --architecture 'all' \
    --depends "default-jre" \
    \
    -s dir \
    -t deb \
    jobson-deb-${VERSION}/usr=/
