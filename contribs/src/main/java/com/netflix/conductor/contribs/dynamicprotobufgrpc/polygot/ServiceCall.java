package com.netflix.conductor.contribs.dynamicprotobufgrpc.polygot;

import com.netflix.conductor.contribs.dynamicprotobufgrpc.polygot.copieddynamic.ChannelFactory;
import com.netflix.conductor.contribs.dynamicprotobufgrpc.polygot.copieddynamic.CompositeStreamObserver;
import com.netflix.conductor.contribs.dynamicprotobufgrpc.polygot.copieddynamic.DynamicGrpcClient;
import com.netflix.conductor.contribs.dynamicprotobufgrpc.polygot.copiedio.LoggingStatsWriter;
import com.netflix.conductor.contribs.dynamicprotobufgrpc.polygot.copiedio.MessageReader;
import com.netflix.conductor.contribs.dynamicprotobufgrpc.polygot.copiedio.MessageWriter;
import com.netflix.conductor.contribs.dynamicprotobufgrpc.polygot.copiedio.Output;
import com.netflix.conductor.contribs.dynamicprotobufgrpc.polygot.copiedprotobuf.ProtoMethodName;
import com.netflix.conductor.contribs.dynamicprotobufgrpc.polygot.copiedprotobuf.ProtocInvoker;
import com.netflix.conductor.contribs.dynamicprotobufgrpc.polygot.copiedprotobuf.ServiceResolver;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HostAndPort;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat.TypeRegistry;
import com.netflix.conductor.contribs.dynamicprotobufgrpc.protogen.ConfigProto;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/** Makes a call to an endpoint, rendering the result */
public class ServiceCall {
  private static final Logger logger = LoggerFactory.getLogger(ServiceCall.class);

  /** Calls the endpoint specified in the arguments */
  public static void callEndpoint(
          String jsonInput,
          Output output,
          ConfigProto.ProtoConfiguration protoConfig,
          Optional<String> endpoint,
          Optional<String> fullMethod,
          Optional<Path> protoDiscoveryRoot,
          Optional<Path> configSetPath,
          ImmutableList<Path> additionalProtocIncludes,
          ConfigProto.CallConfiguration callConfig) {


    // TODO These are parameter checks
//    Preconditions.checkState(endpoint.isPresent(), "--endpoint argument required");
//    Preconditions.checkState(fullMethod.isPresent(), "--full_method argument required");
//    validatePath(protoDiscoveryRoot);
//    validatePath(configSetPath);
//    validatePaths(additionalProtocIncludes);

    HostAndPort hostAndPort = HostAndPort.fromString(endpoint.get());
    ProtoMethodName grpcMethodName =
            ProtoMethodName.parseFullGrpcMethodName(fullMethod.get());
    ChannelFactory channelFactory = ChannelFactory.create(callConfig);

    logger.info("Creating channel to: " + hostAndPort.toString());
    Channel channel;
    // TODO don't worry about auth now
//    if (callConfig.hasOauthConfig()) {
//      channel = channelFactory.createChannelWithCredentials(
//              hostAndPort, new OauthCredentialsFactory(callConfig.getOauthConfig()).getCredentials());
//    } else {
//
//    }

    channel = channelFactory.createChannel(hostAndPort);

    // Fetch the appropriate file descriptors for the service.
    final FileDescriptorSet fileDescriptorSet;
    Optional<FileDescriptorSet> reflectionDescriptors = Optional.empty();
    // TODO assume remote server didn't turn on reflection
//    if (protoConfig.getUseReflection()) {
//      reflectionDescriptors =
//              resolveServiceByReflection(channel, grpcMethodName.getFullServiceName());
//    }

    // TODO can we use .class file here ?
    if (reflectionDescriptors.isPresent()) {
      logger.info("Using proto descriptors fetched by reflection");
      fileDescriptorSet = reflectionDescriptors.get();
    } else {
      try {
        fileDescriptorSet = ProtocInvoker.forConfig(protoConfig).invoke();
        logger.info("Using proto descriptors obtained from protoc");
      } catch (Throwable t) {
        throw new RuntimeException("Unable to resolve service by invoking protoc", t);
      }
    }

    // Set up the dynamic client and make the call.
    ServiceResolver serviceResolver = ServiceResolver.fromFileDescriptorSet(fileDescriptorSet);
    MethodDescriptor methodDescriptor = serviceResolver.resolveServiceMethod(grpcMethodName);

    logger.info("Creating dynamic grpc client");
    DynamicGrpcClient dynamicClient = DynamicGrpcClient.create(methodDescriptor, channel);

    // This collects all known types into a registry for resolution of potential "Any" types.
    // TODO what does this do ?
    TypeRegistry registry = TypeRegistry.newBuilder()
            .add(serviceResolver.listMessageTypes())
            .build();


    // TODO here it reads the inputs
    ImmutableList<DynamicMessage> requestMessages =
            MessageReader.forString(jsonInput,
                    methodDescriptor.getInputType(), registry).read();

    // TODO here it defines the output writers
    StreamObserver<DynamicMessage> streamObserver = CompositeStreamObserver.of(
            new LoggingStatsWriter(), MessageWriter.create(output, registry));

    // TODO we retrieve the output, this is what we should do
    logger.info(String.format(
            "Making rpc with %d request(s) to endpoint [%s]", requestMessages.size(), hostAndPort));
    try {
      dynamicClient.call(requestMessages, streamObserver, callOptions(callConfig)).get();
    } catch (Throwable t) {
      throw new RuntimeException("Caught exception while waiting for rpc", t);
    }
  }

  private static CallOptions callOptions(ConfigProto.CallConfiguration callConfig) {
    CallOptions result = CallOptions.DEFAULT;
    if (callConfig.getDeadlineMs() > 0) {
      result = result.withDeadlineAfter(callConfig.getDeadlineMs(), TimeUnit.MILLISECONDS);
    }
    return result;
  }
}
