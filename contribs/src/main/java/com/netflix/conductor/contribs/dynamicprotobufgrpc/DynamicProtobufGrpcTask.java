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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
import java.io.IOException;
import java.io.PrintStream;
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

    static final String MISSING_REQUEST = "Missing HTTP request. Task input MUST have a '" + REQUEST_PARAMETER_NAME + "' key with DynamicGrpcTask.Input as value. See documentation for DynamicGrpcTask for required input parameters";

    static final String INPUT_PARSE_ERROR = "Error parsing input Json string with DynamicGrpcTask.Input";

    static final String OUTPUT_PARSE_ERROR = "Error parsing output from Grpc response";

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
        // TODO what we get is an object ?
        Object request = task.getInputData().get(requestParameter);

        if(request == null) {
            task.setReasonForIncompletion(MISSING_REQUEST);
            task.setStatus(Status.FAILED);
            return;
        }

        Input input = objectMapper.convertValue(request, Input.class);

        ConfigProto.ProtoConfiguration protoConfig = ConfigProto.ProtoConfiguration.newBuilder()
                .setUseReflection(false)
                .setProtoDiscoveryRoot(config.getContribDynamicGrpcProtoDiscoveryRoot())
                .build();

        // TODO this should be configurable per call
        ConfigProto.CallConfiguration callConfig = ConfigProto.CallConfiguration.newBuilder()
                .setUseTls(false)
                .setDeadlineMs(500)
                .build();


        // It grows automatically, might introduce additional overhead
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = Output.forStream(new PrintStream(baos));

        String inputStr = "";
        try {
            inputStr = objectMapper.writeValueAsString(input.jsonBody);
        } catch (JsonProcessingException e) {
            logger.error("Error parsing DyanmicGrpcTask input", e);
            task.setReasonForIncompletion(INPUT_PARSE_ERROR);
            task.setStatus(Status.FAILED);
            return;
        }
        ServiceCall.callEndpoint(
                inputStr,
                output,
                protoConfig,
                Optional.of(input.endpoint),
                Optional.of(input.fullService),
                null,
                null,
                null,
                callConfig);


        // Once the call is returned, the outputstream has finished writing
        String result = baos.toString();
        // TODO how to map the output to
        Map<String, Object> outputObj = null;
        try {
            outputObj = objectMapper.readValue(result, Map.class);
        } catch (JsonProcessingException e) {
            logger.error("Error parsing grpc result", e);
            task.setReasonForIncompletion(OUTPUT_PARSE_ERROR);
            task.setStatus(Status.FAILED);
            return;
        }
        task.getOutputData().put("response", outputObj);
        logger.info("The output of calling the endpoint is : {}", result);
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

    public static class Input {
        // TODO need to define this class
        // TODO can we do validation on the json input format ?
        private String endpoint;	//host:port ; ip:port , etc

        private String fullService; //xxx.AbcService/MethodName

        private int timeoutInMillis;

        private Map<String, Object> jsonBody;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getFullService() {
            return fullService;
        }

        public void setFullService(String fullService) {
            this.fullService = fullService;
        }

        public int getTimeoutInMillis() {
            return timeoutInMillis;
        }

        public void setTimeoutInMillis(int timeoutInMillis) {
            this.timeoutInMillis = timeoutInMillis;
        }

        public Map<String, Object> getJsonBody() {
            return jsonBody;
        }

        public void setJsonBody(Map<String, Object> jsonBody) {
            this.jsonBody = jsonBody;
        }
    }

//    public static class JsonAsStringDeserializer extends JsonDeserializer<String> {
//        @Override
//        public String deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
//            TreeNode tree = jsonParser.getCodec().readTree(jsonParser);
//            return tree.toString();
//
//        }
//    }
}
