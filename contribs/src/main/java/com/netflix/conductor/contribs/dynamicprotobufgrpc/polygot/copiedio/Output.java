package com.netflix.conductor.contribs.dynamicprotobufgrpc.polygot.copiedio;



import com.netflix.conductor.contribs.dynamicprotobufgrpc.protogen.ConfigProto;
import com.netflix.conductor.contribs.dynamicprotobufgrpc.protogen.ConfigProto.OutputConfiguration.Destination;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * A one-stop-shop for output of the binary. Supports writing to logs, to streams, to files, etc.
 */
public interface Output extends AutoCloseable {
  /** Writes a single string of output. */
  void write(String content);

  /** Writes a line of content. */
  void writeLine(String line);

  /** Writes a blank line. */
  void newLine();

  /**
   * Creates a new {@link OutputImpl} instance for the supplied config. The retruned instance must ]
   * be closed after use or written content could go missing.
   */
  public static Output forConfiguration(ConfigProto.OutputConfiguration outputConfig) {
    Destination destination = outputConfig.getDestination();
    switch(destination) {
      case STDOUT:
        return new OutputImpl(OutputImpl.PrintStreamWriter.forStdout());
      case FILE:
        Path filePath = Paths.get(outputConfig.getFilePath());
        return new OutputImpl(OutputImpl.PrintStreamWriter.forFile(filePath));
      case LOG:
        return new OutputImpl(new OutputImpl.LogWriter(LoggerFactory.getLogger("Output")));
      default:
        throw new IllegalArgumentException("Unrecognized output destination " + destination);
    }
  }

  public static Output forStream(PrintStream printStream) {
    return new OutputImpl(OutputImpl.PrintStreamWriter.forStream(printStream));
  }
}
