package flow.benchmark;

public abstract class BenchmarkTestBase {

  public int iterations = 0;

  public abstract void setup() throws Exception;

  public abstract void teardown() throws Exception;

  public abstract void measure() throws Exception;
}
