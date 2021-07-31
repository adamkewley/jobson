#!/usr/bin/env bash

set -e

if [[ -z "${JOBSON_TAG}" ]]; then
    echo "JOBSON_TAG environment variable is not set: this is required for deployment"
    exit 1
fi

if [[ ! -d "jobson-docs/target" ]]; then
    echo "jobson-docs/target: does not exist: you probably need to build the docs"
    exit 1
fi

git config --global user.email "travis@travis-ci.org"
git config --global user.name  "Travis CI"

git fetch origin gh-pages:gh-pages
git worktree add gh-pages gh-pages

rm -rf gh-pages/*
tar xfz jobson-docs/target/jobson-docs-${JOBSON_TAG}.tar.gz
mv jobson-docs-${JOBSON_TAG}/* gh-pages

cd gh-pages
touch .nojekyll
cd -

# TODO: git add -A
# TODO: git commit -m "Updated docs for ${JOBSON_TAG}"
# TODO: git remote add origin-pages https://${GH_TOKEN}@github.com/adamkewley/jobson.git > /dev/null 2>&1
# TODO: git push origin-pages gh-pages
