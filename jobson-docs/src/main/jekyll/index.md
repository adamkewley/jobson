---
layout: default
title: Jobson
---

Jobson is a webserver that turns command-line applications into a job
system with a standard REST and websocket API. It's designed to
automate the most common steps required to set up a web service,
allowing developers to focus on making standard command-line
applications.

Jobson was designed to be flexible about changing data types and
implementation languages. Those attributes also make it a useful
component in rapidly-evolving batch systems, DevOps platforms, and
microservices.


- [GitHub](https://github.com/adamkewley/jobson)
- [Install Guide](https://github.com/adamkewley/jobson)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Job Specs](#job-specs)
- [HTTP API](#http-api)


# <a name="getting-started"></a> Getting Started

This guide assumes you have
[installed Jobson](https://github.com/adamkewley/jobson).

## Viewing Subcommands

Jobson's command-line interface is where setup, debugging, and bootup
happens. Those commands can be found from a terminal:

```bash
$ jobson --help
usage: java -jar jobson-0.0.11.jar
       [-h] [-v] {server,check,new,generate,users,validate,run} ...

positional arguments:
  {server,check,new,generate,users,validate,run}
                         available commands

optional arguments:
  -h, --help             show this help message and exit
  -v, --version          show the application version and exit
```

The same pattern applies to subcommands:

```bash
$ jobson new --help
usage: java -jar jobson-0.0.11.jar
       new [--demo] [-h]

generate a new jobson deployment in the current working directory

optional arguments:
  --demo                 Generate application with a demo spec (default: false)
  -h, --help             show this help message and exit
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



## <a name="configuration"></a> Configuration

Jobson is configured entirely through standard filesystem structures
and plaintext files contained in the deployment directory. There are
no surprise files hidden elsewhere (yet ;)).

### `config.yml`: Main Configuration File

A standard YAML file that is used by many of Jobson's commands
(e.g. `serve`, `generate`). It contains everything you would expect a
top-level configuration file to contain: data locations, server ports,
authentication configuration, job queue behavior, etc.

See [config.yml](#config-yml) for more details.


### `specs/`: Job Specs

A directory that contains the [job specs](#job-specs) hosted by the
Jobson server. Each subdirectory in `specs/` is a job spec hosted by
Jobson. A job spec ID—as exposed via the Jobson API—is derived from
the subdirectory's name. For example, a job spec at
`specs/foo/spec.yml` would result in `foo` being exposed via the
Jobson API.


### `jobs/`: Job Data

A directory that contains job data. The data associted with each job
request (inputs, timestamps, outputs) is persisted here under a
subdirectory named `{job-id}`.

**Note:** Although job folders are designed to be easy for 3rd-party
  scripts to read, their structure is not yet stable. Don't go
  building something big on the assumption that they are stable.


### `wds/`: Temporary Working Directories

A directory that contains runtime working directories. Jobson
generates a unique job ID for each successful job request. The working
directory used at runtime by the application is persisted in this
directory under a subdirectory named `{job-id}`.

Before a job executes, Jobson creates a clean working directory and
copies all dependencies, file arguments, etc. into it. Jobson then
runs the application in that working directory. This execution model
helps support:

- **Job concurrency**: each job gets its own working directory, so
  concurrent applications are less likely to accidently clobber
  eachother's temporary files and outputs.
  
- **Debugging**: If a job fails, a developer can inspect the working
  directory used by that particular job.

Jobson does not need a working directory after an application has
finised executing. After finishing, Jobson copies any outputs (as
specified in the [job spec](#job-specs)) to the `jobs/` folder.


### `users`: Authorized System Users

A plaintext file that contains users authorized to use the Jobson API
when `basic` authorization (see configuration documentation) is
enabled.

This file should not be edited directly. Instead, the `users` command
should be used to add or modify entries in the file.




TODO: Link to external page


# <a name="config-yml"></a> `config.yml`

`config.yml` is the top-level configuration file for Jobson. It is a
standard YAML file.

**Note**: Relative paths are resolved relative to `config.yml`

## Top-Level Fields

| Key | Default | Description |
| `specs:` | `specs/` | Path to the job specs directory |
| `jobs:` | `jobs/` | Path to the jobs directory |
| `workingDirs:` | (see below) | Path to the temporary working directories |
| `users:` | (see below) | An object containing the users configuration |
| `authentication:` | (see below) | An object containing the authentication configuration |
| `execution:` | (see below) | An object containing the execution configuration |


## `workingDirs`: Working Directory Configuration

Configuration for working directories. Each process spawned by Jobson launches in its own working
directory.

| Key | Default | Description |
| `dir:` | `wds/` | Path the directory that holds working directories |
| `removeAfterExecution:` | (see below) | Configuration for removing working directories after execution |


### `removeAfterExecution`: Policy for Removing Working Directories

| Key | Default | Description |
| `enabled:` | `true` | Indicates whether Jobson should remove working directories after execution |


## `users:`: Users Configuration

| Key | Default | Description |
| `file` | `users` | Path to the `users` file. Used when `basic` authentication is enabled |


## `authentication:`: Authentication Configuration

The relevant `authentication:` fields change based on what `type:` of
authentication that was specified. `guest` auth has different
configuration requriements from `jwt` auth, for example.

| Key | Default | Description |
| `type:` | `basic` | The type of authentication to use. Valid values are `basic`, `guest`, and `jwt`. |
| `*` | N/A | Other keys in `authentication:` depend on what `type:` was specified (see below) |


### `type: guest`: Guest Authentication Configuration

With `guest` authentication, the server will accept all incoming and
assign them a username of `guestUserName`.

| Key | Default | Description |
| guestUserName | guest | The username to assign to all requests |


### `type: basic`: HTTP Basic Authentication Configuration

With `basic` authentication, the server will use a HTTP Basic
([RFC 7617](https://tools.ietf.org/html/rfc7617)) authentication
scheme to collect a username+password pair from clients. Those
credentials will then be authenticated against entries in Jobson's
`users` file (see TODO). Valid credentials shall be permitted to use
the API. Invalid credentials shall be rejected.

| Key | Default | Description |
| realm | JobsonBasicAuth | The "realm" given during the basic auth scheme. For web-browser clients, this is usually displayed as a string in the popup dialog |


### `type: jwt`: Stateless JSON Web Token (JWT) Authentication Configuration

With `jwt` authentication, the server will use stateless JWTs
([RFC 7519](https://tools.ietf.org/html/rfc7519)), which allow clients
to authenticate themselves externally. When enabled, the Jobson server
expects clients to set an `Authorization:` HTTP header with a value of
`Bearer {json-web-token}`. The Jobson server will accept the
credentials in the token provided the token was signed with
`secretKey` (below). If the header is missing, or mis-signed, Jobson
shall reject the request.

| Key | Default | Description |
| secretKey: | (no default) | Should be a base64-encoded string. The signature algorithm used by Jobson is "HS512" (from [here](https://github.com/jwtk/jjwt/blob/master/src/main/java/io/jsonwebtoken/SignatureAlgorithm.java)), which is a HMAC, SHA-512 algorithm |


## `execution:`: Execution Configuration

| Key | Default | Description |
| `maxConcurrentJobs:` | 10 | The number of applications that Jobson is allowed to run concurrently. Jobs are queued if there there are currently more than this number of applications running. |
| `delayBeforeForciblyKillingJobs:` | PT10S | An [ISO 8601](https://en.wikipedia.org/wiki/ISO_8601#Durations) duration string that specifies how long Jobson should wait after sending a `SIGINT` to an application (see [signals](http://man7.org/linux/man-pages/man7/signal.7.html)) before sending a `SIGKILL`. A `SIGKILL` is guaranteed to kill an application, but might result in a harsh exit. Some applications can intelligently handle `SIGINT`s, allowing them to cleanup resources, but might take time to perform cleanup. |


# <a id="http-api"></a> Http API

TODO: The documentation is in the code as Swagger annotations but
needs to be generated.
