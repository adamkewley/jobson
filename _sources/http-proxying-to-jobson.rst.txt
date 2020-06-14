HTTP Proxying to Jobson
=======================

The main ``jobson`` java server hosts a standard JSON API that is
accessed via HTTP. The default port for the JSON API is 8080, which
can be changed in the `server configuration
<server-configuration.html>`__.

In standard production deployments, a ``jobson`` server is typically
hosted behind a a reverse proxy such as `nginx
<https://www.nginx.com/>`__, which adds HTTP encryption, load
balancing, or path matching. Path matching is how ``jobson-ui`` is
integrated with ``jobson``: ``jobson-ui`` can be made to prefix all
API call paths to ``jobson`` with (e.g.) ``/api``, which the reverse
proxy can then use to forward traffic to a ``jobson`` server.

The server also hosts a websocket server, which is used to stream
stdio updates directly to clients as they happen (e.g. ``jobson-ui``
uses this to update the console log dynamically). The websocket API is
not strictly required to use ``jobson`` - ``jobson-ui`` should
function fine without websockets, but won't dynamically update.


Nginx
-----

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


Apache
------

*Thanks to Odgen Brash for testing + documenting this*.

* Install ``apache2`` as normal

* Update your virtual host configuration to proxy API requests to a
  running ``jobson`` server:

.. literalinclude:: _static/apache2-example-config
    :linenos:

* Enable required mods:

.. code:: bash

    a2enmod rewrite proxy proxy_http

* Restart ``apache2``:

.. code:: bash

    apache2ctl restart
