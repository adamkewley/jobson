Workspaces
==========

Jobson is configured through standard plaintext configuration files and
directories. All the necessary files (e.g. ``config.yml``, ``specs/``,
``jobs/``) are, by default, kept together in the same directory after
running the ``jobson new`` command. That directory is what we call a
"workspace".

Top-Level Files/Directories
---------------------------

``config.yml``: Main Configuration File
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

A standard YAML file that is used by many of Jobson's commands (e.g.
``serve``, ``generate``). It contains everything you would expect a
top-level configuration file to contain: data locations, server ports,
authentication configuration, job queue behavior, etc.

See `config.yml <#config-yml>`__ for more details.

``specs/``: Job Specs
~~~~~~~~~~~~~~~~~~~~~

A directory that contains the `job specs <#job-specs>`__ hosted by the
Jobson server. Each subdirectory in ``specs/`` is a job spec hosted by
Jobson. A job spec ID—as exposed via the Jobson API—is derived from the
subdirectory's name. For example, a job spec at ``specs/foo/spec.yml``
would result in ``foo`` being exposed via the Jobson API.

``jobs/``: Job Data
~~~~~~~~~~~~~~~~~~~

A directory that contains job data. The data associted with each job
request (inputs, timestamps, outputs) is persisted here under a
subdirectory named ``{job-id}``.

**Note:** Although job folders are designed to be easy for 3rd-party
scripts to read, their structure is not yet stable. Don't go building
something big on the assumption that they are stable.

``wds/``: Temporary Working Directories
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

A directory that contains runtime working directories. Jobson generates
a unique job ID for each successful job request. The working directory
used at runtime by the application is persisted in this directory under
a subdirectory named ``{job-id}``.

Before a job executes, Jobson creates a clean working directory and
copies all dependencies, file arguments, etc. into it. Jobson then runs
the application in that working directory. This execution model helps
support:

-  **Job concurrency**: each job gets its own working directory, so
   concurrent applications are less likely to accidently clobber
   eachother's temporary files and outputs.

-  **Debugging**: If a job fails, a developer can inspect the working
   directory used by that particular job.

Jobson does not need a working directory after an application has
finised executing. After finishing, Jobson copies any outputs (as
specified in the `job spec <#job-specs>`__) to the ``jobs/`` folder.

``users``: Authorized System Users
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

A plaintext file that contains users authorized to use the Jobson API
when ``basic`` authorization (see configuration documentation) is
enabled.

This file should not be edited directly. Instead, the ``users`` command
should be used to add or modify entries in the file.
