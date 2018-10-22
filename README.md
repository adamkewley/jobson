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


# Superfast Runthrough (Debian)

See [quickstart](TODO) for more details.

```bash
apt install openjdk-8-jre
wget NIX_DIST
tar xvf NIX_DIST && cd NIX_DIST
bin/jobson new --demo
bin/jobson generate spec my_spec
emacs specs/my_spec/spec.yml
bin/jobson serve config.yml
```


# Documentation

TOC TODO

- Overview
- Install
- Quickstart
- Configuration
- UI
