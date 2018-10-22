Jobson
======

Jobson is a webserver that automates the most common steps required to
build a web service around a batch application.

How It Works
------------

Jobson's core abstraction is the `job spec <specs.html>`__, which
describes an application. An example job spec would be:

.. literalinclude:: _static/basic-example-job-spec.yml
    :linenos:
    :language: yaml
      

Using declarative job specs enables Jobson to automatically generate a
HTTP/websocket API for the job, validate requests to run the job,
execute the job as a subprocess, and persist the job's output. It's able
to host any standard command-line application; usually, with no
modifications to the application itself.

Subprocess Execution
~~~~~~~~~~~~~~~~~~~~

Internally, Jobson uses the information contained in `job
specs <specs.html>`__ to
`fork <http://man7.org/linux/man-pages/man2/fork.2.html>`__ a child
process. This means that, to Jobson, the application is a black box
that:

1. Is launched by the host operating system with some command-line
   arguments and dependencies (files, scripts, etc.)

2. Runs for some undetermined amount of time, perhaps producing
   continuous outputs via pipes (e.g. ``stdout``, ``stderr``)

3. Exits with an exit code

4. *Maybe* produces output files as a side-effect

With this approach, Jobson is able to execute any application, written
in any langugage, with sandboxing (processes don't share memory or
threads).

HTTP/Websocket API
~~~~~~~~~~~~~~~~~~

Internally, Jobson uses the information contained in `job
specs <specs.html>`__ to generate a structured API which *requires*
clients to provide the correct information (inputs, login, etc.). The
API exposes jobs, both running and available, via a standard
HTTP/websocket API. The API is also declarative, allowing clients to
automatically enquire about required inputs.

Because of this approach, clients (e.g. `Jobson
UI <https://github.com/adamkewley/jobson-ui>`__) are entirely shielded
from the underlying execution mechanism. Clients just submit standard
JSON requests to a HTTP API and retrieve results/outputs through that
same API. This decoupling means that application developers can
radically change an application (e.g. to a different language) without
breaking downstream clients.

Getting Started
---------------

View the `quickstart <quickstart.html>`__ guide to get started with
Jobson.


.. toctree::
    :maxdepth: 1
    :caption: Contents:

    quickstart
    specs
    commandline
    server-configuration
    template-expressions
    workspaces
    HTTP API <apidocs/index.html#://>
