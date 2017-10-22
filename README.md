# Jobson

[![Build Status](https://travis-ci.org/adamkewley/jobson.svg?branch=master)](https://travis-ci.org/adamkewley/jobson)

Jobson is a web server that can turn command-line applications into
a job system that manages:

- Authentication (guest, HTTP Basic, custom)
- Input collection
- Input validation
- Request IDing
- Request persistence
- Request Queueing
- Request Timestamping
- Server-side execution (`fork(2)`)
- Output forwarding (websockets)
- Output persistence (stdout, stderr, files)


All Jobson needs is a YAML file that describes the application:

```yaml
name: Trivial Application

description: >

  Echoes supplied message to stdout


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

Jobson was developed to generate a standard HTTP API that is simple,
can be changed *very* easily (via spec files), and contains enough
information (names, descriptions) for frontends to provide a decent
user experience.

[Jobson UI](https://github.com/adamkewley/jobson-ui), a sister project,
automatically generates a full web frontend from the Jobson API.


# Build

Built with `Maven` and `JDK 1.8+`. From the project directory:

```bash
mvn package
```

The Jobson fat JAR (`jobson-x.x.x.jar`) and runscript (`jobson`) will be
packed into `target/`. The remainder of this README assumes those
files are kept together and are on the `PATH`.


# QuickStart

This generates an **unauthenticated** standard deployment with a demo job spec:

```bash
jobson new --demo
jobson serve config.yml
```

See API documentation (below) to play with the API or, better, host a
[Jobson UI](https://github.com/adamkewley/jobson-ui) so you can *see* the
API.


# SlowerStart

This generates an authenticated standard deployment with your own spec.

```bash
jobson new

# edit config.yml to use basic auth

# with basic auth enabled, you need to add a user
jobson users add -p password exampleuser

jobson generate spec trivial

# edit specs/trivial/spec.yml

jobson serve config.yml
```


# SlowestStart

Same as above, but with debugging.

```bash
jobson new

# edit config.yml to use basic auth

jobson users add -p password exampleuser

jobson generate spec trivial

# edit specs/trivial/spec.yml

# check for basic errors
jobson validate spec trivial

# generate a request that would be sent to the Jobson API
jobson generate request trivial > dummy-req.json

# edit dummy-req.json

# run the application locally, as if it were ran by the full Jobson stack
jobson run dummy-req.json

# assuming everything looks ok:
jobson serve config.yml
```


# Use the API

With `basic` authentication, Jobson uses HTTP basic authentication (RFC 2617)
to authorize requests.

## cURL

```bash
# The API equivalent of running "jobson run dummy-req.json"
curl --user exampleuser:password --data @dummy-req.json localhost:8080/v1/jobs

# view/query jobs
curl --user exampleuser:password localhost:8080/v1/jobs
curl --user exampleuser:password localhost:8080/v1/jobs?query=somejobname
curl --user exampleuser:password localhost:8080/v1/jobs?query=somejobname&page=1&pagesize=10

# view a specific job
curl --user exampleuser:password localhost:8080/v1/jobs/{id}
curl --user exampleuser:password localhost:8080/v1/jobs/{id}/stdout
curl --user exampleuser:password localhost:8080/v1/jobs/{id}/stderr

# view spec(s)
curl --user exampleuser:password localhost:8080/v1/specs
curl --user exampleuser:password localhost:8080/v1/specs/trivialapplication

# get current user ID
curl --user exampleuser:password localhost:8080/v1/users/current
```

## Javascript (web)

```javascript
var req = new XMLHttpRequest();
req.addEventListener("load", function() {
  console.log(req.responseText);
});
req.open("GET", "http://localhost:8080/v1/jobs");
req.setRequestHeader("Authorization", "Basic " + btoa("exampleuser:password"));
req.send();
```


