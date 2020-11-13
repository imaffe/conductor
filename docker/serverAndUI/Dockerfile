#
# conductor:serverAndUI - Netflix conductor server and UI
#
FROM openjdk:8-jdk

MAINTAINER Netflix OSS <conductor@netflix.com>

# Make app folders
RUN mkdir -p /app/config /app/logs /app/libs /usr/local/conductor-grpc-protos

# Startup script(s)
COPY ./bin /app

# Configs
COPY ./config /app/config


COPY ../../contribs/src/main/resources/proto/*.proto /usr/local/conductor-grpc-protos/

# Get the GNU C to run protoc

RUN apk add --no-cache \
        perl \
        wget \
        openssl \
        ca-certificates \
        libc6-compat \
        libstdc++ && \
        ln -s /lib/libc.musl-x86_64.so.1 /lib/ld-linux-x86-64.so.2

# Get all the dependencies
RUN apt-get update -y \
  && apt-get -y install git curl \

  # Chmod scripts
  && chmod +x /app/startup.sh \

  # Get node
  && curl -sL https://deb.nodesource.com/setup_10.x |  bash - \
  && apt-get install -y nodejs build-essential

# Get and install conductor
RUN git clone https://github.com/Netflix/conductor.git \
  && cd conductor \
  && ./gradlew build -x test \

  # Get Server Jar
  && mv ./server/build/libs/conductor-server-*-all.jar /app/libs/ \

  # Get UI project
  && mv ./ui /app \

  # Install UI packages
  && cd /app/ui \
  && npm install \
  && npm run build --server \

  # Go back to root
  && cd / \

  # Clean up
  && rm -rf conductor


EXPOSE 5000 8080

CMD ["/app/startup.sh"]
ENTRYPOINT ["/bin/bash"]
