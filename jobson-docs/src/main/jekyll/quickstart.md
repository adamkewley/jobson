---
layout: default
title: Quickstart
---

Jobson's setup is inspired by [Ruby on Rails](rubyonrails.org), where
the command-line is used to quickly generate and launch
convention-over-configuration projects (in Jobson,
[workspaces](workspaces.md)).


## 1) Install

Follow Jobson's main [README](https://github.com/adamkewley/jobson)
for installation steps.


## 2) Test Jobson's Command-Line Interface

This guide uses Jobson's [command-line](commandline.html) to get
going. Ensure you can run it; for example, by showing its help pages:

```bash
$ jobson --help
```


## 3) Generate a Workspace

[Workspaces](workspaces.html) are Jobson's
convention-over-configuration approach to organizing its configuration
(`config.yml`), persistence (`jobs/`) and so on. To generate a
workspace, run:

```bash
$ jobson new
```


## Create a Jobson Deployment

Jobson is designed to be driven exclusively by standard filesystem
structures and plaintext files. The `new` command generates a
barebones deployment containing those files:

```bash
$ jobson new --demo
create    config.yml
create    users
create    specs
create    specs/demo
create    specs/demo/spec.yml
create    specs/demo/demo-script.sh
create    specs/demo/demo-dependency
create    jobs
create    wds
```

Details about the Jobson's directory structure can be found
[here](#dir-structure).


## Generate a Job Spec

[Job specs](#job-specs) are standard YAML files that describe your
application. They are held in the `specs/` folder
([details](#configuration)). The `generate spec` command generates a
new job spec:

```bash
$ jobson generate spec foo
create    specs/foo
create    specs/foo/spec.yml
```

For the sake of this guide, lets assume you want Jobson to host a
python script takes two inputs, prints something to the standard
output (via `print`), and writes an output file:

```python
# specs/foo/foo.py

import os

first_name = os.sys.argv[0]
second_name = os.sys.argv[1]

print(first_name + " " + second_name)

with open("output.txt", "w") as f:
    f.write("Some output data\n")
```

In order to turn this script into a web API, Jobson needs a job spec
that describes the script. Lets edit `specs/foo/spec.yml` to do that:


```yaml
# specs/foo/spec.yml

name: A python script in the Jobson documentation
description: >
  
  A job that prints the provided first and second name, followed by
  writing some text to an output file.

expectedInputs:

- id: firstName
  type: string
  name: First Name
  
- id: secondName
  type: string
  name: Second Name
  
execution:
  application: python
  arguments:
  - foo.py
  - ${inputs.firstName}
  - ${inputs.secondName}
  dependencies:
  - source: foo.py
    target: foo.py

expectedOutputs:
- id: outputFile
  path: output.txt
```

The spec gives Jobson:

- Basic documentation of the application

- Expected inputs and their types

- A description of how the application is executed
  
- Expected outputs produced by the application

Further details about what's possible in job specs can be found
[here](#job-specs).


## Validate the Job Spec

The `foo` job spec can be validated by Jobson to check for basic
syntactical errors. The `validate spec` command will exit with no
output if your job spec is syntactically valid. Run it from your main
jobson deployment folder:

```bash
$ jobson validate spec foo
$ 
```


## Generate a Request Against the Job Spec

Running an application via Jobson is different from running an
application from the command line (see [discussion](#job-specs)), so
we need to generate a job request. The `generate request` command
generates a standard JSON request against a spec:

```bash
$ jobson generate request foo
{
  "spec" : "foo",
  "name" : "Adipisci voluptatum vel dolore omnis delectus.",
  "inputs" : {
    "firstName" : "Et sint qui nam tempore.",
    "secondName" : "Maxime dolores aut est."
  }
}
```

Jobson has generated placeholder text (e.g. `Et sint qui nam
tempore.`) for the inputs. The generated JSON matches the structure of
requests as sent via the Jobson HTTP API (specifically, `POST
/v1/jobs`).


## Run a Job Request Against the Job Spec


Although the job spec is syntactically correct, it may still fail at
runtime, so it's good practice to run a request against the spec.

The `run` command runs a job request locally:

```bash
$ jobson generate request foo > request.json
$ jobson run request.json
Et sint qui nam tempore. Maxime dolores aut est.
```

The `generate request` command generated lorem-ipsum text for
`firstName` and `lastName`, which was forwarded into our python script
(`foo.py`) and printed out.

Although it isn't obvious, the `run` command ran `request.json`
through the entire Jobson stack in order to verify that nothing
breaks. As a convenience feature, it redirected the the standard
output, standard error, and exit code from the application back to the
command-line, which lets you debug runtime errors more easily.

We've now created a job spec, validated it, and ran it locally, all
that's left is to host it.


## Boot the Server

Jobson is ultimately a webserver that hosts a REST and websocket API
for the applications that are described by job specs. With a working
job spec in place, we're ready to boot a server. The `serve` command
should be ran from the jobson deployment folder:

```bash
$ jobson serve config.yml 
# .
# .
# ...many log messages
```

The server is then running, which you can verify with a HTTP tool such
as `curl`:

```bash
$ curl localhost:8080/v1/
{"_links":{"specs":{"href":"/v1/specs"},"current-user":{"href":"/v1/users/current"},"jobs":{"href":"/v1/jobs"}}}
```

The `request.json` generated for the `run` command is an entirely
valid API request. Therefore, you can also `POST` it via the HTTP API:

```bash
$ curl --data @request.json -H 'Content-Type: application/json' localhost:8080/v1/jobs
{"id":"svpj5ppevn","_links":{"outputs":{"href":"/v1/jobs/svpj5ppevn/outputs"},"inputs":{"href":"/v1/jobs/svpj5ppevn/inputs"},"self":{"href":"/v1/jobs/svpj5ppevn"},"spec":{"href":"/v1/jobs/svpj5ppevn/spec"}}}
```

More details about the Jobson HTTP API are available
[here](#http-api).

There you have it: a standard HTTP + websocket API for `foo.py`. Now
that a server is running, downstream clients can use the API to post
job requests to the server, which will validate the request is valid
(e.g. "it has a `firstName` string field"), run the application, and
collect outputs - all while handling authentication, IDing,
persistence, queueing, concurrency, etc.




## What's Next?

Now that you've seen the general idea behind Jobson, there's several
steps you can take:

- **Add a user interface**: The
  [Jobson UI](https://github.com/adamkewley/jobson-ui) project uses
  job specs to generate a website that can be used by anyone with a
  browser.
  
- **Customize the server**: See [Configuration documentation](#configuration)
  
- **Learn about Job Specs**: See [Job Specs documentation](#job-specs)

- **Use the API**: See [API Documentation](#api)