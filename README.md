# jobson

[Documentation](https://adamkewley.github.io/jobson)

[![Build Status](https://travis-ci.org/adamkewley/jobson.svg?branch=master)](https://travis-ci.org/adamkewley/jobson)

A webserver that turns command-line applications into a job system.
Once hosted, [Jobson UI](https://github.com/adamkewley/jobson-ui) can then
automatically generate UIs for any hosted application.

Jobson needs a YAML description file for each application it hosts:

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


# Install

- Ensure you have `java` 8 on your platform (e.g. `sudo apt install openjdk-8-jre`)
- Find an appropriate [release](https://github.com/adamkewley/jobson/releases) for your platform
- Install it, if it's pre-packed for your platform (e.g. `.deb` on ubuntu). Otherwise, manually
  install it (below)

### Manual Install

- Download a binary tarball for your platform (e.g. `*nix`) from the
  [releases](https://github.com/adamkewley/jobson/releases) page (e.g. `jobson-nix-0.0.15.tar.gz`)
- Unzip the tarball wherever you want jobson to be installed (e.g. `/opt`)
- (optional) Add the top-level `jobson` executable to the PATH (e.g. `ln -s jobson /usr/local/bin/jobson`)


# Quickstart

```bash
jobson new --demo
jobson serve config.yml
```

See [documentation](https://adamkewley.github.io/jobson) for a comprehensive
walkthrough.



# Build

Building the Jobson jar for development requires `maven` and `jdk` (8+). For
example:

```bash
apt install maven openjdk-8-jdk

mvn package
```

Building an entire release (packages, documentation, etc.) additionally
requires `ruby` and `bundler`, along with some gems. For example:

```bash
apt install maven openjdk-8-jdk ruby ruby-bundler
gem install fpm

mvn package -P release
```

See `.travis.yml` for a "clean" build example.
