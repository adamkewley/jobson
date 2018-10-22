# jobson-deb

Debian packaging for Jobson

This module builds a `.deb` package using assets from the `jobson-nix` tarball.
The package tries to follow standard debian conventions (w.r.t file locations, etc.).

Exceptions:

- Repackages (from `jobson-nix`) java dependencies to `/usr/share/jobson` rather 
  than the FSH standard `/usr/share/java` location because `jobson-deb` this isn't 
  an official debian package and there's a risk that the java libraries could 
  clobber an official jar in `/usr/share/java`. 
  
- Repackages (from `jobson-nix`) the top-level `bin/jobson` script to use the
  alternative jar location
  
- Jobson uses isolated workspaces, rather than system-level configuration, so this
  package won't install a server config at (e.g.) `/etc/jobson/config.yml` because
  it's expected that users will create isolated workspaces using the `jobson new`
  command somewhere.
