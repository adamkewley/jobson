# jobson-nix

Linux-style distribution of the jobson platform.

The distribution assembles all major artifacts in the `jobson` platform
into a into a standard [FSH](https://en.wikipedia.org/wiki/Filesystem_Hierarchy_Standard)-like hierarchy.
It's designed to include everything, so that downstream *nix consumers 
can just download this and have everything needed use, deploy, or
repackage the platform.

- `jobson` bash script: `bin/jobson`
- `jobson` jar: `share/java/jobson-x.x.x.jar` 
- `jobson` java dependencies: `share/java` 
- `jobson-docs`: `share/doc/jobson`
- `jobson-ui`: `share/jobson/ui`
