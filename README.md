# Jobson

Turn command-line applications into webapps.

Jobson lets developers share command-line applications over the web
without modification by providing an abstraction layer over process 
forking, HTTP API generation, standard IO, and websockets. 

It uses a high-level declarative spec (YAML) that describes applications:

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

Jobson uses these specs to automatically generate and manage:

- A HTTP API (`/v1/jobs`, `/v1/specs`, `/v1/jobs/{id}/stderr`. etc.)

- A dynamic websocket API (`/v1/jobs/events`,
`/v1/jobs/{id}/stderr/updates`, etc.)

- Request validation

- A persistence layer (specs, jobs, metadata, outputs)

- An authentication layer

Jobson and [Jobson UI](https://github.com/adamkewley/jobson-ui) were
developed on a project where researchers and developers work together 
on a variety of data pipelines. We spent a lot of time handling data 
requests, explaining how to install applications, explaining how to
run applications, tracing requests, etc. Attempts to engineer around the 
problem with bespoke web servers worked, but those systems were
brittle and needed redevelopment every time a new workflow came along. 

Jobson was developed to generate a self-explanatory, standard API 
that is simple, can be changed *very* easily (via spec files), and 
contained enough information (names, descriptions) for frontends to
automatically generate user-friendly UIs.


# Build

Built with `Maven` and `JDK 1.8+`. From the project directory:

```bash
mvn package
```

The Jobson fat JAR (`jobson-x.x.x.jar`) and runscript (`jobson`) will be
packed into `target/`. The remainder of this README assumes those
files are kept together and available on the `PATH`.


# Quickstart

```bash
jobson new

jobson useradd exampleuser

# set the password
jobson passwd exampleuser

# generate a new spec
jobson generatespec trivialapplication

# (edit specs/trivialapplication/spec.yml accordingly)

# (optional)
jobson validate trivialapplication

# (optional): generate a dummy request
jobson generatereq trivialapplication > dummy-req.json

# (optional): run the request locally
jobson run dummy-req.json

# boot the server - port configured in config.yml
jobson serve config.yml
```


# Use the API

This early release of Jobson uses HTTP basic authentication to 
authorize requests.

## cURL

```bash
# post a job request against a hosted spec
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

# get current user ID (used by browsers)
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

