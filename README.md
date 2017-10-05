# Jobson

Turn any command-line application into a web-based job system.

Jobson lets developers share command-line applications over the web
without modification. It does so by providing an abstraction layer
over process forking, HTTP API generation, standard IO, and
websockets.

Jobson uses declarative job specs (YAML) to describe what an
application is, what inputs it needs, and how to run it:

```yaml
name: Trivial Application
description: Echoes client request

expectedInputs:

- id: msg
  type: string
  name: Message to Echo
  description: The message to echo
  default: Hello, world!

execution:
  application: echo
  args:
  - $request
```

Declarative specs enable Jobson to automatically generate and manage:

- A HTTP API (`/v1/jobs`, `/v1/specs`, `/v1/jobs/{id}/stderr`. etc.)

- A dynamic websocket API (`/v1/jobs/events`,
`/v1/jobs/{id}/stderr/updates`, etc.)

- Request validation

- A persistence layer (specs, jobs, metadata, outputs)

- An authentication layer

Jobson and Jobson UI were developed on a project where researchers and
developers work together on a variety of data pipelines. We spent a
lot of time handling data requests, explaining how to install
applications, explaining how to run applications, tracing requests,
etc.

Attempts to systemize the process with bespoke web servers worked, but
those systems were brittle and needed redevelopment every time a new
workflow came along. Jobson was developed to generate a
self-explanatory, standard API that is simple, can be changed *very*
easily (spec files), and contained enough information (names,
descriptions) for frontends to generate easy-to-use UIs.


# Build

Build with `Maven` and `JDK 1.8+`:

```bash
mvn package
```

The jobson JAR (`jobson-x.x.x.jar`) and runscript (`jobson`) will be
packed into `target/`. The remainder of this README assumes those
files are kept together and available on the `PATH`.


# Quickstart

```bash
# create new deployment in current dir
jobson new

# add a user
jobson useradd exampleuser

# set the user's password
jobson passwd exampleuser

# generate a new spec
jobson generatespec trivialapplication

# (edit the spec accordingly)

# (optional): validate the spec
jobson validate trivialapplication

# (optional): run a request against the spec locally - pipes stdio to console
jobson generatereq trivialapplication > dummy-req.json
jobson run dummy-req.json

# boot the server
jobson serve jobsoncfg.yml
```

The server then hosts a standard HTTP and websocket API (default port
8080).


# Use the API

Jobson uses HTTP basic authentication to authorize requests. Assuming
a username = `exampleuser` and password = `password`:

## curl

```bash
curl --user exampleuser:password --data @dummy-req.json localhost:8080/v1/jobs

curl --user exampleuser:password localhost:8080/v1/jobs
curl --user exampleuser:password localhost:8080/v1/jobs?query=somejobname
curl --user exampleuser:password localhost:8080/v1/jobs?query=somejobname&page=1&pagesize=10

curl --user exampleuser:password localhost:8080/v1/jobs/somejobid
curl --user exampleuser:password localhost:8080/v1/jobs/somejobid/stdout
curl --user exampleuser:password localhost:8080/v1/jobs/somejobid/stderr

curl --user exampleuser:password localhost:8080/v1/specs
curl --user exampleuser:password localhost:8080/v1/specs/trivialapplication

curl --user exampleuser:password localhost:8080/v1/users/current
```

## javascript (web)

```javascript
var req = new XMLHttpRequest();
req.addEventListener("load", function() {
  console.log(req.responseText);
});
req.open("GET", "http://localhost:8080/v1/jobs");
req.setRequestHeader("Authorization", "Basic " + btoa("exampleuser:password"));
req.send();
```

