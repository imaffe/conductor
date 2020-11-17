#
# conductor:server - Netflix conductor server
#

# 0. Builder stage

FROM gradle:5.6.4-jdk11 AS cache
COPY . /conductor
WORKDIR /conductor
RUN mkdir -p /home/gradle/cache_home
ENV GRADLE_USER_HOME /home/gradle/cache_home
COPY gradlew build.gradle versionsOfDependencies.gradle /home/gradle/java-code/
WORKDIR /home/gradle/java-code
RUN gradle clean build -i --stacktrace


FROM openjdk:8-jdk AS builder

MAINTAINER Netflix OSS <conductor@netflix.com>
# move the cache
COPY --from=cache /home/gradle/cache_home /home/gradle/.gradle
# Copy the project directly onto the image
COPY . /conductor
WORKDIR /conductor

# Build the server on run
RUN ./gradlew build -x test

# 1. Bin stage
FROM openjdk:8-jre-alpine

MAINTAINER Netflix OSS <conductor@netflix.com>


# Get the GNU C to run protoc

RUN apk add --no-cache \
        curl \
        perl \
        wget \
        openssl \
        ca-certificates \
        libc6-compat \
        libstdc++ && \
        ln -s /lib/libc.musl-x86_64.so.1 /lib/ld-linux-x86-64.so.2


# Make app folders
RUN mkdir -p /app/config /app/logs /app/libs /usr/local/conductor-grpc-protos

RUN curl -O https://arthas.aliyun.com/arthas-boot.jar && \
    mv arthas-boot.jar /app


# Copy the project directly onto the image
COPY --from=builder /conductor/docker/server/bin /app
COPY --from=builder /conductor/docker/server/config /app/config
COPY --from=builder /conductor/server/build/libs/conductor-server-*-all.jar /app/libs
COPY --from=builder /conductor/docker/server/proto/ /usr/local/conductor-grpc-protos/

# Copy the files for the server into the app folders
RUN chmod +x /app/startup.sh

EXPOSE 8080
EXPOSE 8090

CMD [ "/app/startup.sh" ]
ENTRYPOINT [ "/bin/sh"]
