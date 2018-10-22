FROM ubuntu:16.04


# Install 3rd-party dependencies
RUN apt update && apt install -y \
    nginx \
    default-jre \
    supervisor

# Install jobson debian package
COPY target/jobson.deb /tmp
RUN dpkg -i /tmp/jobson.deb
RUN rm /tmp/jobson.deb

# Configure nginx
COPY default.conf /etc/nginx/conf.d/default.conf
RUN rm -rf /etc/nginx/sites-enabled/default

# Configure supervisord
COPY supervisord.conf /etc/supervisord.conf

# Configure OS to have a 'jobson' account
RUN groupadd -r jobson && useradd --no-log-init -r -g jobson jobson
RUN mkdir -p /home/jobson && chown jobson:jobson /home/jobson

# Configure 'jobson' account with a jobson workspace
USER jobson
RUN  cd /home/jobson && jobson new --demo  # so a blank img boot shows *something*
USER root



EXPOSE 80
CMD ["supervisord", "--configuration", "/etc/supervisord.conf", "--nodaemon"]
