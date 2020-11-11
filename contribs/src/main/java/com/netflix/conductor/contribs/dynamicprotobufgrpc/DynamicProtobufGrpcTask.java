/*
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.conductor.contribs.dynamicprotobufgrpc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.Task.Status;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.contribs.dynamicprotobufgrpc.polygot.ServiceCall;
import com.netflix.conductor.contribs.dynamicprotobufgrpc.polygot.copiedio.Output;
import com.netflix.conductor.contribs.dynamicprotobufgrpc.protogen.ConfigProto;
import com.netflix.conductor.contribs.http.RestClientManager;
import com.netflix.conductor.core.config.Configuration;
import com.netflix.conductor.core.execution.WorkflowExecutor;
import com.netflix.conductor.core.execution.tasks.WorkflowSystemTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Viren
 * Task that enables calling another http endpoint as part of its execution
 */
@Singleton
public class DynamicProtobufGrpcTask extends WorkflowSystemTask {

    public static final String REQUEST_PARAMETER_NAME = "grpc_request";

    // static final String MISSING_REQUEST = "Missing HTTP request. Task input MUST have a '" + REQUEST_PARAMETER_NAME + "' key with HttpTask.Input as value. See documentation for HttpTask for required input parameters";

    private static final Logger logger = LoggerFactory.getLogger(DynamicProtobufGrpcTask.class);

    public static final String NAME = "DYNAMIC_PROTOBUF_GRPC";

    private TypeReference<Map<String, Object>> mapOfObj = new TypeReference<Map<String, Object>>(){};

    private TypeReference<List<Object>> listOfObj = new TypeReference<List<Object>>(){};

    protected ObjectMapper objectMapper;

    protected RestClientManager restClientManager;

    protected Configuration config;

    private String requestParameter;

    private static final String TMP_JSON_INPUT = "{\"latitude\": 407838351, \"longitude\": -746143763}";

    @Inject
    public DynamicProtobufGrpcTask(RestClientManager restClientManager,
                    Configuration config,
                    ObjectMapper objectMapper) {
        this(NAME, restClientManager, config, objectMapper);
    }

    public DynamicProtobufGrpcTask(String name,
                    RestClientManager restClientManager,
                    Configuration config,
                    ObjectMapper objectMapper) {
        super(name);
        this.restClientManager = restClientManager;
        this.config = config;
        this.objectMapper = objectMapper;
        this.requestParameter = REQUEST_PARAMETER_NAME;
        logger.info("DynamicProtobufGrpcTask initialized...");
    }

    @Override
    public void start(Workflow workflow, Task task, WorkflowExecutor executor) {
        logger.info("DynamicProtobufGrpcTask Start gets called");
//        ConfigProto.OutputConfiguration outputConfig = ConfigProto.OutputConfiguration.newBuilder()
//                .setDestination(ConfigProto.OutputConfiguration.Destination.LOG)
//                .setFilePath("C:\\Users\\affezhang\\IdeaProjects\\java_protobuf_demo2\\src\\main\\proto\\output.json")
//                .build();


        URL res = getClass().getClassLoader().getResource("input.json");
        File file = null;
        try {
            file = Paths.get(res.toURI()).toFile();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        String parentPath = file.getParent();


        ConfigProto.ProtoConfiguration protoConfig = ConfigProto.ProtoConfiguration.newBuilder()
                .setUseReflection(false)
                .setProtoDiscoveryRoot(Paths.get(parentPath, "proto").toString())
                .build();

        ConfigProto.CallConfiguration callConfig = ConfigProto.CallConfiguration.newBuilder()
                .setUseTls(false)
                .setDeadlineMs(50000)
                .build();


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = Output.forStream(new PrintStream(baos));


        ServiceCall.callEndpoint(

                TMP_JSON_INPUT,
                output,
                protoConfig,
                Optional.of("localhost:8980"),
                Optional.of("routeguide.RouteGuide/GetFeature"),
                null,
                null,
                null,
                callConfig);


        logger.info("The output of calling the endpoint is : {}", baos.toString());
        task.setStatus(Status.COMPLETED);
    }


    @Override
    public boolean execute(Workflow workflow, Task task, WorkflowExecutor executor) {
        return false;
    }

    @Override
    public void cancel(Workflow workflow, Task task, WorkflowExecutor executor) {
        task.setStatus(Status.CANCELED);
    }

    // TODO HttpTask set this to false, we can set this true
    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public int getRetryTimeInSecond() {
        return 60;
    }
}
