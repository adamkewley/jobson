# Jobson UI

A web frontend for [Jobson](https://github.com/adamkewley/jobson) API.

<p align="center">
  <img src="/docs/screenshot.jpg?raw=true" height="500" alt="Jobson Screenshot" />
</p>

[screencast](https://www.youtube.com/watch?v=W9yfpqWiyUg)


# Install

tl;dr (longer version [here](#longer-install-guide)):

- Download a [release](https://github.com/adamkewley/jobson-ui/releases)
- Unzip it onto a webserver (e.g. [nginx](https://www.nginx.com/))
- Configure the webserver to reverse-proxy requests beginning with `/api` to a
  running [Jobson](https://github.com/adamkewley/jobson) server ([nginx example](/docs/nginx-example-config))
- *Optional*: Configure the webserver to reverse-proxy websocket requests ([nginx example](/docs/nginx-example-config))
  . This is only required if you want realtime updates in the UI.
- Configure the UI by editing `config.json` (if necessary)


# Develop

Jobson UI is developed with `node` (`v6.11.4`+) and `npm`.

To build a release of Jobson UI from source, which writes the built assets to `public/`:

```bash
npm install
npm run build
```


To run a development server, which dynamically updates as the source code is edited (best
development experience):

```bash
npm install
npm run start
```

**Note**: This assumes an actual Jobson server is running at `localhost:8080`.



## <a name="longer-install-guide"></a> Longer Install Guide

Jobson UI builds into a set of standard, static, web assets. Those assets need
to be hosted on a standard webserver (e.g. [nginx](https://www.nginx.com/)).

By default, all Jobson API requests made by the UI are prefixed with `/api`. This prefix can
be changed by editing `config.json`. Your webserver of choice should be configured with
a reverse proxy that forwards all requests beginning with `/api` to a
[Jobson](https://github.com/adamkewley/jobson) server. Jobson UI uses websockets to listen to
events (updates to stdio, new jobs, etc.), so the webserver should also be configured to
forward websockets if you want dynamic updates in the UI.

This step-by-step guide assumes [nginx](https://www.nginx.com/) is the webserver:

- Download a [release](https://github.com/adamkewley/jobson-ui/releases) of Jobson UI
- Unzip the assets to the webserver's asset folder (e.g. `/var/www/jobson-ui`)
- Install [nginx](https://www.nginx.com/) (e.g. `sudo apt install nginx`)
- Create an nginx config file for the site. [This](/docs/nginx-example-config) is a starting
  point
- Save the config file, ready for nginx (e.g. at `/etc/nginx/sites-available/jobson-ui`)
- Enable the config file (e.g. `cd /etc/nginx/sites-enabled && ln -s ../sites-available/jobson-ui jobson-ui`)
- Reload nginx (`nginx -s reload`)
