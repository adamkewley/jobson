# jobson-docker

Docker packaging for Jobson.

Builds a standard docker image that pre-integrates `jobson` with `jobson-ui` (with
`nginx`).


# Filesystem

- A standard `jobson` workspace is created for the image at `/home/jobson`. Users
  should copy/remount this with their own configuration
- `nginx` configuration is in the standard location (e.g. `/etc/nginx`) if users 
  want to reconfigure the webserver (e.g. with HTTPS)
  
# Execution

- Top-level uses `supervisord` to supervise `jobson` and `nginx`
- All execution performed by the `jobson` user. Users of this image should ensure
  that `jobson` can execute the underling application 

# Ports

- Exposes `nginx` on port 80
- Forwards any requests beginning with `/api` to a `jobson` server listening on
  port 8080 (not exposed)