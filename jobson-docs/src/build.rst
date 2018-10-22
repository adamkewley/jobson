Developer's Guide
=================

Building the Jobson jar for development requires ``maven`` and ``jdk`` (8+). For
example:

.. code:: bash

	  apt install maven openjdk-8-jdk
	  mvn package

Building an entire release (packages, documentation, etc.) additionally
requires ``ruby`` and ``bundler``, along with some gems. For example:


.. code:: bash

	  apt install maven openjdk-8-jdk ruby ruby-bundler
	  gem install fpm

	  mvn package -P release

See ``.travis.yml`` for a "clean" build example.

Jobson UI is developed with ``node`` (``v6.11.4``\ +) and ``npm``.

To build a release of Jobson UI from source, which writes the built
assets to ``public/``:

.. code:: bash

    npm install
    npm run build

To run a development server, which dynamically updates as the source
code is edited (best development experience):

.. code:: bash

    npm install
    npm run start

**Note**: This assumes an actual Jobson server is running at
``localhost:8080``.
