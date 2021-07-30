#!/usr/bin/env bash

set -xeuo pipefail

sudo apt-get update
sudo apt-get install -yf python3 python3-pip python3-setuptools python3-venv nodejs
sudo gem install fpm
sudo pip3 install -r jobson-docs/requirements.txt
