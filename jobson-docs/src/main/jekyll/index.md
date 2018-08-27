---
layout: default
title: Overview
---

Jobson is a webserver that automates the most common steps required to
set up a web service around a batch application, which enables
developers to focus on their core application.



## How It Works

Jobson uses job specs to host a HTTP/websocket API that can launch
subprocesses server-side.


### Job Specs

[Job specs](specs.html) are what Jobson uses to understand each
application. An example job spec would be:

```yaml
name: Example Job Spec

description: A job that echoes the provided first name
  
expectedInputs:

- id: firstName
  type: string
  name: First Name
  description: Your first name
  default: Jeff

execution:

  application: echo
  arguments:
  - ${inputs.firstName}
  
```

Jobson uses job specs to automatically generate a HTTP/websocket API
for the job, validate requests to run the job, execute the job as a
subprocess, and persist the job's output.

Because of this declarative approach, Jobson is able to host any
command-line application. Usually, with no modifications to the
original application.


### Subprocess Execution

Jobson uses the host operating system to execute jobs. Internally,
Jobson uses the information contained in [job specs](specs.html) to
[fork](http://man7.org/linux/man-pages/man2/fork.2.html) a child
process. This means that, to Jobson, the application is effectively a
black box that:

1. Is launched by the host operating system with some command-line
   arguments and dependencies (files, scripts, etc.)

2. Runs for some undetermined amount of time, perhaps producing
   continuous outputs via pipes (e.g. `stdout`, `stderr`)

3. Exits with an exit code

4. *Maybe* produces output files as a side-effect

Because of this approach, Jobson is able to execute any application,
written in any langugage, in parallel, with operating-system-level
sandboxing (from failures, memory leaks, etc.).


### HTTP/Websocket API

Jobson exposes jobs, both running and available, via a standard
HTTP/websocket API. Internally, Jobson uses the information contained
in [job specs](specs.html) to generate a structured API which
*requires* clients to provide the correct information (inputs, login,
etc.). The API is also declarative, allowing clients to automatically
enquire about required inputs.

Because of this approach, clients
(e.g. [Jobson UI](https://github.com/adamkewley/jobson-ui)) are
entirely shielded from the underlying execution mechanism. Clients
just submit standard JSON requests to an API and retrieve
results/outputs through that same API. This decoupling means that
application developers can radically change an application (e.g. to a
different language) without affecting downstream clients.


## Getting Started

View the [quickstart](quickstart.html) guide to get started with
Jobson.
