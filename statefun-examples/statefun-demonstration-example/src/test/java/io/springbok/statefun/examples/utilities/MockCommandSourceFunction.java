package io.springbok.statefun.examples.utilities;

import io.springbok.statefun.examples.demonstration.generated.Command;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

import java.util.ArrayList;
import java.util.Iterator;

public class MockCommandSourceFunction implements SourceFunction<Command> {

  private ArrayList<Command> commands;
  private volatile boolean isRunning = true;
  public int runTimeMS = 8000;

  public MockCommandSourceFunction() {}

  public MockCommandSourceFunction(ArrayList<String> commands) {
    this.commands = new ArrayList<>();
    commands.forEach(
        command -> {
          Command command_message = Command.newBuilder().setCommand(command).build();
          this.commands.add(command_message);
        });
  }

  @Override
  public void run(SourceContext<Command> sourceContext) throws InterruptedException {

    if (!(commands == null)) {
      Iterator<Command> commandIterator = commands.iterator();

      while (isRunning && commandIterator.hasNext())
        for (Command command : commands) {
          sourceContext.collect(commandIterator.next());
        }
      sourceContext.close();
      Thread.sleep(runTimeMS);
    }
  }

  @Override
  public void cancel() {
    isRunning = false;
  }
}
