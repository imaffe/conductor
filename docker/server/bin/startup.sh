#!/bin/sh
# startup.sh - startup script for the server docker image

echo "Starting Conductor server"

# Start the server
cd /app/libs
echo "Property file: $CONFIG_PROP"
echo $CONFIG_PROP
export config_file=

echo "Log4j file: $LOG4J_PROP"
echo $LOG4J_PROP
export log4j_file=

if [ -z "$CONFIG_PROP" ];
  then
    echo "Using an in-memory instance of conductor";
    export config_file=/app/config/config-local.properties
  else
    echo "Using '$CONFIG_PROP'";
    export config_file=/app/config/$CONFIG_PROP
fi

if [ -z "$LOG4J_PROP" ];
  then
    export log4j_file=/app/config/log4j.properties
  else
    echo "Using '$LOG4J_PROP'";
    export log4j_file=/app/config/$LOG4J_PROP
fi

java -jar -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9102 \
-Dcom.sun.management.jmxremote.authenticate=false \
-Dcom.sun.management.jmxremote.ssl=false \
-Djava.rmi.server.hostname=127.0.0.1 \
conductor-server-*-all.jar $config_file $log4j_file
