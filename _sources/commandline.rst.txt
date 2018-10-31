Command-Line Interface
======================

The command-line interface (CLI) for Jobson, ``jobson``, contains
commands aimed at day-to-day development and administration of Jobson.
The CLI is available once jobson has been
`installed <install.html>`__.



Running the CLI via a Docker Container
--------------------------------------

The ``jobson`` CLI can be used from outside a container by first entering the workspace (``cd /home/jobson``) then
executing the commands. For example, adding a ``basic`` auth user via a container:

.. code:: bash

      docker start jobson-container
      docker exec -ti jobson-container sh -c "cd /home/jobson && jobson users add config.yml -p PASSWORD USERNAME"



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



Adding a new User (basic auth)
------------------------------

If Jobson is configured to use ``basic`` authentication in the `server config <server-configuration.html>`_ :

.. code:: yaml

    # config.yml
    authentication:
      type: basic

The ``users`` subcommand can be used to add new users to the system:

.. code:: bash

    $ jobson users add config.yml -p PASSWORD USERNAME


This command appends an entry to the file pointed at by the ``users.file``
setting in the server config (``config.yml``). The entry is hashed in a
similar way to how Linux passwords are hashed, preventing the password from
being read:

.. code:: bash

    $ cat users
    USERNAME:basic:$6$XbPs6uOo$7AYilI2.iL84jLxXqb10vnzGygtXWCy1W27EUU7AhbKrDrtHGSI1jTRCIlUUmMhDzdwZ0sS7vm7iBrJ1VV6JB.
