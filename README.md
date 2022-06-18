# jobson <img src="logo.svg" align="right" alt="" width="128" height="128" />

> A platform for transforming command-line applications into a job service

<a href="https://github.com/adamkewley/jobson/actions">
  <img src="https://github.com/adamkewley/jobson/actions/workflows/continuous-integration-workflow.yml/badge.svg" />
</a> 

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.5148300.svg)](https://doi.org/10.5281/zenodo.5148300)


![ui video demo](jobson-docs/src/ui-animation.gif)


📖 [Documentation](https://adamkewley.github.io/jobson), ⭐️ [Demo](https://jobson.adamkewley.com)

Jobson is a platform (backend webserver, frontend UI, and command-line
client) that transforms command-line applications into a web-ready job
service. Jobson's goal is to make it easy to share existing
applications across the web without requiring a lot of server
expertise. It was started in 2017 as internal software for handling
scientific data requests for the [Gaia](https://sci.esa.int/web/gaia)
satellite mission. Because it solves a general problem (turning
arbitrary CLI applications into a web service) it was subsequently
open-sourced in 2018.

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
wget https://github.com/adamkewley/jobson/releases/download/1.0.14/jobson-nix-1.0.14.tar.gz
tar xvf jobson-nix-1.0.14.tar.gz
export PATH=$PATH:jobson-nix-1.0.14/bin

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
