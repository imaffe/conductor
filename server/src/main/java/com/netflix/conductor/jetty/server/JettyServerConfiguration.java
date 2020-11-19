package com.netflix.conductor.jetty.server;

import com.netflix.conductor.core.config.Configuration;

public interface JettyServerConfiguration extends Configuration {
    String ENABLED_PROPERTY_NAME = "conductor.jetty.server.enabled";
    boolean ENABLED_DEFAULT_VALUE = true;

    String PORT_PROPERTY_NAME = "conductor.jetty.server.port";
    int PORT_DEFAULT_VALUE = 8080;

    String MAX_THREAD_NUM_PROPERTY_NAME = "conductor.jetty.server.threads.max";
    int MAX_THREAD_NUM_VALUE = 200;

    String MIN_THREAD_NUM_PROPERTY_NAME = "conductor.jetty.server.threads.min";
    int MIN_THREAD_NUM_VALUE = 200;


    String QUEUE_TYPE_PROPERTY_NAME = "conductor.jetty.server.queue.type";
    String QUEUE_TYPE_NUM_VALUE = "monitor";

    String QUEUE_SIZE_PROPERTY_NAME = "conductor.jetty.server.queue.size";
    int QUEUE_SIZE_NUM_VALUE = 100;


    String JOIN_PROPERTY_NAME = "conductor.jetty.server.join";
    boolean JOIN_DEFAULT_VALUE = true;

    default boolean isEnabled(){
        return getBooleanProperty(ENABLED_PROPERTY_NAME, ENABLED_DEFAULT_VALUE);
    }

    default int getPort() {
        return getIntProperty(PORT_PROPERTY_NAME, PORT_DEFAULT_VALUE);
    }

    default boolean isJoin(){
        return getBooleanProperty(JOIN_PROPERTY_NAME, JOIN_DEFAULT_VALUE);
    }


    default int getMaxThreadNum(){
        return getIntProperty(MAX_THREAD_NUM_PROPERTY_NAME, MAX_THREAD_NUM_VALUE);
    }

    default int getMinThreadNum(){
        return getIntProperty(MIN_THREAD_NUM_PROPERTY_NAME, MIN_THREAD_NUM_VALUE);
    }

    default String getQueueType(){
        return getProperty(QUEUE_TYPE_PROPERTY_NAME, QUEUE_TYPE_NUM_VALUE);
    }



    default int getQueueSize(){
        return getIntProperty(QUEUE_SIZE_PROPERTY_NAME, QUEUE_SIZE_NUM_VALUE);
    }
}
