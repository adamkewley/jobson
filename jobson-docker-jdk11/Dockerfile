FROM azul/zulu-openjdk-alpine:11

# Install 3rd-party dependencies
RUN apk update && \
    apk add nginx supervisor && \
    rm -rf /etc/nginx/sites-enabled/default

# Install jobson debian package
COPY target/jobson.tar.gz /tmp
RUN mkdir /tmp/jobson && \
    cd /tmp/jobson && \
    tar -xvf /tmp/jobson.tar.gz --strip-components=1 && \
    mkdir -p /usr/share/jobson && \
    mv /tmp/jobson/share/java /usr/share/jobson/ && \
    mv /tmp/jobson/share/jobson/ui /usr/share/jobson/ && \
    sed -i -E 's#".+/share#"/usr/share/jobson#' /tmp/jobson/bin/jobson && \
    sed -i -E 's#bash#sh#' /tmp/jobson/bin/jobson && \
    mv /tmp/jobson/bin/jobson /usr/bin/ && \
    rm -rf /tmp/jobson.tar.gz /tmp/jobson && \
    mkdir -p /run/nginx

# Configure nginx
COPY default.conf /etc/nginx/conf.d/default.conf

# Configure supervisord
COPY supervisord.conf /etc/supervisord.conf

# Configure OS to have a 'jobson' account
RUN addgroup -S jobson && \
    adduser -S -G jobson jobson && \
    mkdir -p /home/jobson && \
    chown jobson:jobson /home/jobson

# Configure 'jobson' account with a jobson workspace
USER jobson
RUN  cd /home/jobson && jobson new --demo  # so a blank img boot shows *something*
USER root

EXPOSE 80
CMD ["supervisord", "--configuration", "/etc/supervisord.conf", "--nodaemon"]
