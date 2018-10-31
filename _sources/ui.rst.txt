User Interface (Jobson UI)
==========================

The core ``jobson`` server only hosts a JSON/websocket API for
Jobson. ``jobson-ui`` is a set of static web assets that should be
installed separately on a production-grade webserver (e.g. ``apache``,
``nginx``).


Screenshot
----------

.. image:: _static/screenshot.jpg



Overview
--------

.. image:: _static/ui-arch.svg


``jobson-ui`` is a set of static web assets that should be hosted on a
standard webserver. This enables admins to administer the user-facing
API of jobson with their own standard security/caching/authentication
apporaches.

At runtime, browser clients go through the following steps:

* Clients download ``index.html`` (from ``jobson-ui``) from the
  webserver
* ``<script>`` tags in the HTML cause clients to download associated
  javascript from the webserver
* The javascript makes clients download ``config.json`` from the
  webserver
* ``config.json`` contains UI configuration options, including an
  ``apiPrefix`` property, which tells clients what to prefix jobson
  API requests with
* Subsequent ``jobson`` API calls are prefixed with ``apiPrefix``
  (e.g. ``/api/{some-req}``)

  
This guide demonstrates configuring ``nginx`` to serve ``jobson-ui``
as described.


Optional: Boot ``jobson`` somewhere
-----------------------------------

Deploying the UI doesn't require a running ``jobson`` server. However,
for the sake of testing, this guide assumes you have a ``jobson``
server running at ``localhost:8080``:

.. code:: bash

	  jobson serve config.yml  # config.yml is configured to serve on port 8080

	  

Get ``jobson-ui`` Assets
------------------------

Pre-packaged distributions (debian, unix) of the jobson platform
include the assets at ``/share/jobson/ui/html``. Those assets are
"ready to go" and should be copied/linked to an appropriate location
(below).

The `jobson project <https://github.com/adamkewley/jobson>`__ build
also uploads ``jobson-ui`` as an independent artifact. If you just
want the UI, you can download it from the `releases
<https://github.com/adamkewley/jobson/releases>`__ page.


Unpack/Move Assets to Webserver Root
------------------------------------

For this install guide, we will copy the assets to ``/var/www/jobson``:

.. code:: bash

	  cp -r /usr/share/jobson/ui/html /var/www/jobson


	  
Optional: Edit ``config.json``
------------------------------

If you want to change the ``apiPrefix``, edit the UI configuration file:

.. code:: bash

	  nano /var/www/jobson/config.json

This will cause ``jobson`` API requests to go elsewhere. For this
guide, we assume the default value (``/api``).

	  
Configure ``nginx``
-------------------

* Create an nginx config file for the site at
  ``/etc/nginx/sites-available/jobson-ui``:

.. literalinclude:: _static/nginx-example-config
    :linenos:

* Enable the site configuration:

.. code:: bash

   cd /etc/nginx/sites-enabled
   ln -s ../sites-available/jobson-ui jobson-ui

* Reload nginx:

.. code:: bash

   nginx -s reload


   
Optional: Load the UI in a Browser
----------------------------------

Try loading the UI in a browser:

.. code:: bash

	  xdg-open http://localhost

Which, if ``jobson`` is running, should work fine.
