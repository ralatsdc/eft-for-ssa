package io.springbok.eft_for_ssa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.flink.api.common.io.FileInputFormat;
import org.apache.flink.core.fs.FileInputSplit;
import org.apache.flink.core.fs.FileSystem;

public class TrackletFormatter extends FileInputFormat<Tracklet> {
	
	private transient FileSystem fileSystem;
    private transient BufferedReader reader;
    private final String inputPath;
    private String line;

    public TrackletFormatter(String inputPath) {
        this.inputPath = inputPath;
    }

    @Override
    public void open(FileInputSplit inputSplit) throws IOException {
        FileSystem fileSystem = getFileSystem();
        this.reader = new BufferedReader(new InputStreamReader(fileSystem.open(inputSplit.getPath())));
        this.line = reader.readLine();
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
        return line == null;
    }

    @Override
    public Tracklet nextRecord(Tracklet tracklet) throws IOException {

    	tracklet = Tracklet.fromString(line); 
        line = reader.readLine();

        return tracklet;
    }
}
