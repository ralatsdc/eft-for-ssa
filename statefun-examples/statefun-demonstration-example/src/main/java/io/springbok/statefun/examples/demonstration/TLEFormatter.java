package io.springbok.statefun.examples.demonstration;

import org.apache.flink.api.common.io.FileInputFormat;
import org.apache.flink.core.fs.FileInputSplit;
import org.apache.flink.core.fs.FileSystem;
import org.orekit.propagation.analytical.tle.TLE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

public class TLEFormatter extends FileInputFormat<TLE> {

  private transient FileSystem fileSystem;
  private transient BufferedReader reader;
  private final String inputPath;
  private String line1;
  private String line2;

  public TLEFormatter(String inputPath) {
    this.inputPath = inputPath;
  }

  @Override
  public void open(FileInputSplit inputSplit) throws IOException {
    FileSystem fileSystem = getFileSystem();
    this.reader = new BufferedReader(new InputStreamReader(fileSystem.open(inputSplit.getPath())));
    this.line1 = reader.readLine();
    this.line2 = reader.readLine();
  }

  private FileSystem getFileSystem() {
    if (fileSystem == null) {
      try {
        fileSystem = FileSystem.get(new URI(inputPath));
      } catch (URISyntaxException | IOException e) {
        throw new RuntimeException(e);
      }
    }
    return fileSystem;
  }

  @Override
  public boolean reachedEnd() throws IOException {
    return line1 == null;
  }

  @Override
  public TLE nextRecord(TLE tle) throws IOException {

    line1 = reader.readLine();
    line2 = reader.readLine();
    if (TLE.isFormatOK(line1, line2)) {
      tle = new TLE(line1, line2);
    }
    return tle;
  }
}
