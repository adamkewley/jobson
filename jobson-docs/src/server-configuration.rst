Server Configuration
====================

``config.yml`` is the top-level configuration file for Jobson. It is a
standard YAML file.

**Note**: Relative paths are resolved relative to ``config.yml``

Top-Level Fields
----------------

.. list-table:: Top-level fields
    :header-rows: 1

    * - Key
      - Default
      - Description

    * - ``specs:``
      - ``specs/``
      - Path to the job specs directory

    * - ``jobs:``
      - ``jobs/``
      - Path to the jobs directory

    * - ``workingDirs:``
      - (see below)
      - Path to the temporary working directories

    * - ``users:``
      - (see below)
      - An object containing the users configuration

    * - ``authentication:``
      - (see below)
      - An object containing the authentication configuration

    * - ``execution:``
      - (see below)
      - An object containing the execution configuration



``workingDirs``: Working Directory Configuration
------------------------------------------------

Configuration for working directories. Each process spawned by Jobson
launches in its own working directory.

.. list-table:: Working Directory Configuration

    * - Key
      - Default
      - Description

    * - ``dir:``
      - ``wds/``
      - Path the directory that holds working directories

    * - ``removeAfterExecution:``
      - (see below)
      - Configuration for removing working directories after execution



``removeAfterExecution``: Policy for Removing Working Directories
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. list-table::

    * - Key
      - Default
      - Description

    * - ``enabled:``
      - ``true``
      - Indicates whether Jobson should remove working directories after execution


``users:``: Users Configuration
-------------------------------

.. list-table::

    * - Key
      - Default
      - Description

    * - ``file``
      - ``users``
      - Path to the ``users`` file. Used when ``basic`` authentication is enabled


``authentication:``: Authentication Configuration
-------------------------------------------------

The relevant ``authentication:`` fields change based on what ``type:``
of authentication that was specified. ``guest`` auth has different
configuration requriements from ``jwt`` auth, for example.

.. list-table::

    * - Key
      - Default
      - Description

    * - ``type:``
      - ``basic``
      - The type of authentication to use. Valid values are ``basic``, ``guest``, and ``jwt``. Other keys in ``authentication:`` depend on what ``type:`` was specified (see below)


``type: guest``: Guest Authentication Configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

With ``guest`` authentication, the server will accept all incoming and
assign them a username of ``guestUserName``.

.. list-table::

    * - Key
      - Default
      - Description

    * - ``guestUserName``
      - guest
      - The username to assign to all requests


``type: basic``: HTTP Basic Authentication Configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

With ``basic`` authentication, the server will use a HTTP Basic (`RFC
7617 <https://tools.ietf.org/html/rfc7617>`__) authentication scheme to
collect a username+password pair from clients. Those credentials will
then be authenticated against entries in Jobson's ``users`` file (see
TODO). Valid credentials shall be permitted to use the API. Invalid
credentials shall be rejected.

.. list-table::

    * - Key
      - Default
      - Description

    * - ``realm``
      - JobsonBasicAuth
      - The "realm" given during the basic auth scheme. For web-browser clients, this is usually displayed as a string in the popup dialog

``type: jwt``: Stateless JSON Web Token (JWT) Authentication Configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

With ``jwt`` authentication, the server will use stateless JWTs (`RFC
7519 <https://tools.ietf.org/html/rfc7519>`__), which allow clients to
authenticate themselves externally. When enabled, the Jobson server
expects clients to set an ``Authorization:`` HTTP header with a value of
``Bearer {json-web-token}``. The Jobson server will accept the
credentials in the token provided the token was signed with
``secretKey`` (below). If the header is missing, or mis-signed, Jobson
shall reject the request.

.. list-table::

    * - Key
      - Default
      - Description

    * - ``secretKey``
      - (no default)
      - Should be a base64-encoded string. The signature algorithm used by Jobson is "HS512" (from `here <https://github.com/jwtk/jjwt/blob/master/src/main/java/io/jsonwebtoken/SignatureAlgorithm.java>`__), which is a HMAC, SHA-512 algorithm


``execution:``: Execution Configuration
---------------------------------------

.. list-table::

    * - Key
      - Default
      - Description

    * - ``maxConcurrentJobs:``
      - 10
      - The number of applications that Jobson is allowed to run concurrently. Jobs are queued if there there are currently more than this number of applications running.

    * - ``delayBeforeForciblyKillingJobs:``
      - PT10S
      - An `ISO8601 <https://en.wikipedia.org/wiki/ISO_8601#Durations>`__ duration string that specifies how long Jobson should wait after sending a ``SIGINT`` to an application (see `signals <http://man7.org/linux/man-pages/man7/signal.7.html>`__) before sending a ``SIGKILL``. A ``SIGKILL`` is guaranteed to kill an application, but might result in a harsh exit. Some applications can intelligently handle ``SIGINT``\ s, allowing them to cleanup resources, but might take time to perform cleanup.
