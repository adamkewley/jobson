Install
=======

The Jobson project builds distributions (unix, debian) of ``jobson``
that include the Jobson's core libraries, documentation, UI assets,
etc.


Debian OSes (Ubuntu, Mint, etc.)
--------------------------------

The ``jobson`` build produces a ``.deb`` file that can be installed
directly:

.. parsed-literal::

	  wget https://github.com/adamkewley/jobson-1.0-throwaway/releases/download/|release|/jobson_\ |release|\ _all.deb
	  dpkg -i jobson_\ |release|\ _all.deb  # contains dependencies on (e.g.) java
	  apt-get -f install  # resolves dependencies

	  
OSX/Linux (any)
---------------

The ``jobson`` project produces a tarball containing all assets needed
to run jobson on a unix-like OS.

**Note**: You must have ``java`` (8+) installed on your system in order to run ``jobson``

.. parsed-literal::

      wget https://github.com/adamkewley/jobson-1.0-throwaway/releases/download/\ |release|\ /jobson-nix-\ |release|\ .tar.gz
      tar xvf jobson-nix-\ |release|\ .tar.gz

      # optional: add to ~/.bashrc
      export PATH=$PATH:/location/of/unpacked/tarball/bin


Docker
------

The ``jobson`` project builds a Docker image that pre-integrates ``jobson`` and ``jobson-ui`` (with ``nginx``) together.
By default, it will create a demo workspace at `/home/jobson/` inside the container and listen on port 80:

.. parsed-literal::

      docker run --name default-container -p 80:80 adamkewley/jobson-1.0.0-throwaway:\ |release|\

It's recommended that you host the workspace outside the container, so that you can backup job data, upgrade the image,
etc. The easiest way to do this is to copy the default workspace out of the container and create a new container with
a volume mount:

.. parsed-literal::

      HOST_PORT=9090

      # copy out the container's workspace
      docker run --name tmp-jobson-container -d adamkewley/jobson-1.0.0-throwaway:\ |release|\

      docker cp tmp-jobson-container:/home/jobson /host/path/jobson
      docker container rm -f tmp-jobson-container

      # boot a container that uses the out-of-container workspace
      docker run --name jobson-container -p ${HOST_PORT}:80 -v /host/path/jobson:/home/jobson:rw -d adamkewley/jobson-1.0.0-throwaway:\ |release|\


This will boot a docker container that listens on ``HOST_PORT``, which you can browse to.

	  
Windows
-------

The ``jobson`` project does not officially build or support
windows. However, unofficial reports do state that it works on windows
if you manually run the ``jobson-x.x.x.jar`` file:

* Download and install ``java``
* Download jobson unix tarball from `releases <https://github.com/adamkewley/jobson-1.0-throwaway/releases/>`_
* Unpack somewhere (e.g. in ``DIR``)
* Run the jobson CLI with ``java -jar DIR/share/jobson/java/jobson-x.x.x.jar``
