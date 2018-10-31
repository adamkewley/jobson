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
