Developer's Guide
=================

Depending on what you want to build, you'll need (on Ubuntu):

- ``jobson``: ``maven``, ``openjdk-8-jdk``
- ``jobson-deb``: ``build-essential``, ``ruby``, ``ruby-bundler``
- ``jobson-docker``: ``docker``
- ``jobson-docs``: ``python3``, ``python3-pip``
- ``jobson-ui``: ``nodejs``, ``npm``

Overall, installing all dependencies on a fresh machine looks something like:

.. code:: bash

    apt install build-essential maven openjdk-8-jdk ruby ruby-bundler nodejs npm python3 python3-pip
    gem install fpm  # used to make .deb packages
    pip3 install -r jobson-docs/requirements.txt

    # build jobson
    mvn package -P release

See ``.github`` in the ``jobson`` repo for a build example.
