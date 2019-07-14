# jobson

[![Build Status](https://travis-ci.org/adamkewley/jobson.svg?branch=master)](https://travis-ci.org/adamkewley/jobson)

A platform for transforming command-line applications into a job service.

[Documentation](https://adamkewley.github.io/jobson), [Demo](https://adamkewley.com/demos/jobson/show/index.html)


Jobson generates a standard web service from a job specification file:

```yaml
name: Trivial Application
description: Echoes supplied message to stdout

expectedInputs:
- id: message
  type: string
  name: Message
  description: The message to echo
  default: Hello, world!

execution:
  application: echo
  arguments:
  - ${inputs.message}
```

# Quickstart

See
[documentation](https://adamkewley.github.io/jobson/quickstart.html)
for comprehensive walkthrough.

Requires java (8+):

```bash
# install and add to PATH
wget https://github.com/adamkewley/jobson/releases/download/1.0.6/jobson-nix-1.0.6.tar.gz
tar xvf jobson-nix-1.0.5.tar.gz
export PATH=$PATH:jobson-nix-1.0.6/bin

# create demo workspace
jobson new --demo

# generate new spec
jobson generate spec someSpec

# edit
#nano specs/someSpec/spec.yml

# validate 
jobson validate spec someSpec

# host JSON API for running job
jobson serve config.yml
```

See [installation](https://adamkewley.github.io/jobson/install.html)
documentation for details.
