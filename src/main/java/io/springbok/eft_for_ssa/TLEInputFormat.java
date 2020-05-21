package io.springbok.eft_for_ssa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.flink.api.common.io.FileInputFormat;
import org.apache.flink.core.fs.FileInputSplit;
import org.apache.flink.core.fs.FileSystem;
import org.orekit.propagation.analytical.tle.TLE;

import scopt.Read;

public class TLEInputFormat extends FileInputFormat<TLE> {
	
	private transient FileSystem fileSystem;
    private transient BufferedReader reader;
    private final String inputPath;
    private String lineOne;
    private String lineTwo;

    public TLEInputFormat(String inputPath) {
        this.inputPath = inputPath;
    }

    @Override
    public void open(FileInputSplit inputSplit) throws IOException {
        FileSystem fileSystem = getFileSystem();
        this.reader = new BufferedReader(new InputStreamReader(fileSystem.open(inputSplit.getPath())));
        this.lineOne = reader.readLine();
        this.lineTwo = reader.readLine();
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
        return lineOne == null;
    }

    @Override
    public TLE nextRecord(TLE r) throws IOException {
    	r = new TLE(lineOne, lineTwo);
 
        lineOne = reader.readLine();
        lineTwo = reader.readLine();

        return r;
    }

}
