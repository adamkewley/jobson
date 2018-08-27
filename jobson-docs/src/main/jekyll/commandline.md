---
layout: default
title: Command-Line Interface
---

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
