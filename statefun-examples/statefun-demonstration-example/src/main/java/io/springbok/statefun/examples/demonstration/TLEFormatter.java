package io.springbok.statefun.examples.demonstration;

import org.apache.flink.api.common.io.FileInputFormat;
import org.apache.flink.core.fs.FileInputSplit;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.orekit.propagation.analytical.tle.TLE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class TLEFormatter extends FileInputFormat<TLE> {

  private FileSystem fileSystem;
  private transient BufferedReader reader;
  private String line1;
  private String line2;

  public TLEFormatter(Path filePath) {
    super(filePath);
  }

  @Override
  public void open(FileInputSplit inputSplit) throws IOException {
    FileSystem fileSystem = getFileSystem();
    this.reader = new BufferedReader(new InputStreamReader(fileSystem.open(filePath)));
  }

  private FileSystem getFileSystem() {
    if (fileSystem == null) {
      try {
        fileSystem = filePath.getFileSystem();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return fileSystem;
  }

  @Override
  public boolean reachedEnd() {
    return line1 == null;
  }

  @Override
  public TLE nextRecord(TLE tle) throws IOException {

    line1 = reader.readLine();
    if (line1 == null) {
      line2 = null;
    } else {
      line2 = reader.readLine();
    }
    if (TLE.isFormatOK(line1, line2)) {
      tle = new TLE(line1, line2);
    }
    return tle;
  }
}
