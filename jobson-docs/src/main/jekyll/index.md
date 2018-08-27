---
layout: default
title: Overview
---

Jobson is a webserver that automates the most common steps required to
set up a web service around a batch application, which saves
developers time and lets them focus on desgining standard command-line
applications.



## How It Works


### Job Specs

Jobson's core abstraction is the [job spec](specs.html), which it uses
to automatically:

- Generate a HTTP/websocket API for the job
- Validate requests to run the job
- Execute the job
- Persist the job's data (inputs, outputs, logs, etc.)


### Subprocess Execution

Jobson was developed to help researchers handle, track, and manage
complex, long-running batch applications written a variety of
languages/frameworks. Rather than directly integrating with each
language/framework, Jobson executes jobs as subprocesses. This means
that, to Jobson, the application is effectively a black box that:

1. Is launched by the host operating system with some command-line
   arguments and dependencies (files, scripts, etc.)

2. Runs for some undetermined amount of time, perhaps producing
   continuous outputs via pipes (e.g. `stdout`, `stderr`)

3. Exits with an exit code

4. *Maybe* produces output files as a side-effect

Because of this approach, Jobson may execute any standard command-line
application written in any langugage.


### HTTP/Websocket API






At time of writing, all of this data is persisted to Jobson via a
[workspace](), which is a standard filesystem layout.



## Where It's Useful

- Research platforms
- Devops (launching routine tasks, etc.)
- Rapid development



## Getting Started

View the [quickstart](quickstart.html) guide to get started with
Jobson.