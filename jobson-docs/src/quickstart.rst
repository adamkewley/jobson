Quickstart
==========

Jobson's initial setup is inspired by `Ruby on
Rails <//rubyonrails.org>`__, where the command-line is used to generate
and launch convention-over-configuration projects (in Jobson,
`workspaces <workspaces.md>`__).

1) Install
----------

Follow Jobson's main `README <https://github.com/adamkewley/jobson>`__
for installation steps.

2) Test Jobson's Command-Line Interface
---------------------------------------

This guide uses Jobson's command-line to get going. It should be
available if you have installed Jobson correctly. Ensure you can run it,
for example, by showing its help pages:

.. code:: bash

    $ jobson --help

More details about the command-line interface can be found
`here <commandline.html>`__.

3) Generate a Workspace
-----------------------

Workspaces Jobson's way of organizing its configuration and data. The
``new`` command generates a barebones workspace containing the minimum
necessary files to run a Jobson server:

.. code:: bash

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

More details about workspaces can be found `here <workspaces.html>`__.

4) Generate a Job Spec
----------------------

Job specs standard YAML files that describe an application. They are
held in the ``specs/`` folder in a workspace. The ``generate spec``
command generates a new job spec:

.. code:: bash

    $ jobson generate spec foo
    create    specs/foo
    create    specs/foo/spec.yml

More details about job specs can be found `here <specs.html>`__.

5) Edit the Job Spec to match the Application
---------------------------------------------

For the sake of this guide, lets assume you want Jobson to host a python
script takes two inputs, prints something to the standard output (via
``print``), and writes an output file:

.. code:: python

    # specs/foo/foo.py

    import os

    first_name = os.sys.argv[0]
    second_name = os.sys.argv[1]

    print(first_name + " " + second_name)

    with open("output.txt", "w") as f:
        f.write("Some output data\n")

In order to turn this script into a web API, Jobson needs a job spec
that describes the script. Lets edit ``specs/foo/spec.yml`` to do that:

.. code:: yaml

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

This example is describing a job that:

-  Takes two ``string`` inputs (``firstName`` and ``secondName``)

-  Copies ``foo.py`` in the job spec's folder to ``foo.py`` in the
   runtime working directory

-  Executes ``python`` with arguments ``{inputs.firstName}``,
   ``${inputs.secondName}``. These arguments are examples of template
   expressions. In this case, those expressions resolve to the
   ``firstName`` and ``secondName`` inputs provided by clients

-  Checks that the ``python`` script exits with an exit code of ``0``

-  Once the application has exited, checks for a file called
   ``output.txt`` in the application's working directory and persists
   that file under the ID ``outputFile``

More details about `job specs <specs.html>`__ and `template
expressions <template-expressions.html>`__ are available at the links.

5) *Optional*: Validate and Run a Job Against the Job Spec
----------------------------------------------------------

The job spec can be validated by Jobson to check for basic syntactical
errors. The ``validate spec`` command will exit with no output if your
job spec is syntactically valid. Run it from your main jobson deployment
folder:

.. code:: bash

    $ jobson validate spec foo

The ``validate`` command exits silently on success.

For a more in-depth validation step, it's a good idea to run an actual
job request against the job spec to see if any runtime bugs pop up.
Jobson accepts accepts job requests in a JSON format. The
``generate request`` command generates a random standard JSON request
against a spec:

.. code:: bash

    $ jobson generate request foo
    {
      "spec" : "foo",
      "name" : "Adipisci voluptatum vel dolore omnis delectus.",
      "inputs" : {
        "firstName" : "Et sint qui nam tempore.",
        "secondName" : "Maxime dolores aut est."
      }
    }

Jobson has generated placeholder text (e.g.
``Et sint qui nam tempore.``) for the inputs. The generated JSON matches
the structure of requests as sent via the Jobson HTTP API (specifically,
``POST /v1/jobs``).

Although the job spec is syntactically correct, it may still fail at
runtime, so it's good practice to run a request against the spec.

The ``run`` command runs a job request locally:

.. code:: bash

    $ jobson generate request foo > request.json
    $ jobson run request.json
    Et sint qui nam tempore. Maxime dolores aut est.

The ``generate request`` command generated lorem-ipsum text for
``firstName`` and ``lastName``, which was forwarded into our python
script (``foo.py``) and printed out.

Although it isn't obvious, the ``run`` command ran ``request.json``
through the entire Jobson stack in order to verify that nothing breaks.
As a convenience feature, it redirected the the standard output,
standard error, and exit code from the application back to the
command-line, which lets you debug runtime errors more easily.

We've now created a job spec, validated it, and ran it locally, all
that's left is to host it.

8) Boot the Server
------------------

With a working job spec in place, we're ready to boot a server. The
``serve`` command should be ran from the workspace:

.. code:: bash

    $ jobson serve config.yml 

More details about the server configuration (``config.yml``) are
available `here <server-configuration.html>`__.

9) Verify the Server's Working
------------------------------

The server is then running, which you can verify with a HTTP tool such
as ``curl``:

.. code:: bash

    $ curl localhost:8080/v1/
    {"_links":{"specs":{"href":"/v1/specs"},"current-user":{"href":"/v1/users/current"},"jobs":{"href":"/v1/jobs"}}}

The ``request.json`` generated for the ``run`` command is an entirely
valid API request. Therefore, you can also ``POST`` it via the HTTP API:

.. code:: bash

    $ curl --data @request.json -H 'Content-Type: application/json' localhost:8080/v1/jobs
    {"id":"svpj5ppevn","_links":{"outputs":{"href":"/v1/jobs/svpj5ppevn/outputs"},"inputs":{"href":"/v1/jobs/svpj5ppevn/inputs"},"self":{"href":"/v1/jobs/svpj5ppevn"},"spec":{"href":"/v1/jobs/svpj5ppevn/spec"}}}

What's Next?
------------

Now that a server is running, downstream clients can use the API to post
job requests to the server, which will validate the request is valid
(e.g. "it has a ``firstName`` string field"), run the application, and
collect outputs - all while handling authentication, IDing, persistence,
queueing, concurrency, etc.

Now that you've seen the general idea behind Jobson, there's several
steps you can take:

-  **Add a user interface**: The `Jobson
   UI <https://github.com/adamkewley/jobson-ui>`__ project uses job
   specs to generate a website that can be used by anyone with a
   browser.

-  **Customize the server**: See `Server
   Configuration <server-configuration.html>`__

-  **Learn about Job Specs**: See `Job Specs <specs.html>`__
