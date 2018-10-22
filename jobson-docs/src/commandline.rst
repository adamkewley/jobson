Command-Line Interface
======================

The command-line interface (CLI) for Jobson, ``jobson``, contains
commands aimed at day-to-day development and administration of Jobson.
The CLI is available once jobson has been
`installed <https://github.com/adamkewley/jobson>`__.

Viewing Help with ``--help``
----------------------------

Pass the ``--help`` argument to ``jobson`` to view the help
documentation for a particular command.

.. code:: bash

    $ jobson --help
    usage: java -jar jobson-0.0.11.jar
           [-h] [-v] {server,check,new,generate,users,validate,run} ...

    positional arguments:
      {server,check,new,generate,users,validate,run}
                             available commands

    optional arguments:
      -h, --help             show this help message and exit
      -v, --version          show the application version and exit

The ``--help`` argument also works with the other subcommands:

.. code:: bash

    $ jobson new --help
    usage: java -jar jobson-0.0.11.jar
           new [--demo] [-h]

    generate a new jobson deployment in the current working directory

    optional arguments:
      --demo                 Generate application with a demo spec (default: false)
      -h, --help             show this help message and exit

HELP TODO: The in-CLI documentation explains the commands, but having
the documentation here also would be useful to devs.
