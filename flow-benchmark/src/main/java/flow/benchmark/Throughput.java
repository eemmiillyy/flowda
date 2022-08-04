package flow.benchmark;

public class Throughput extends BenchmarkTestBase {

  public int iterations = 5;

  public Throughput() {
    // Double the iterations for a warm up period.
    for (int i = 0; i < iterations * 2; i++) {
      setup();
      measure();
      teardown();
    }
  }

  public void setup() {
    System.out.println("Calling setup");
    // Create kafka client listening to topic
    System.out.println("Created kafka client listening to output topic");
    // Create mysql client
    System.out.println("Created mysql client");
    // Fetch the current count, assign target value
    System.out.println("Fetched the current mysql count");
    // Use mysql client to update database row (Start off with zero of everything)
    System.out.println(
      "Updated row in database by an amount that would lead to base targetValue, test can start"
    );
    // Use mysql client(s) to update database row at rate of 1000 concurrent requests, do not wait for response
    System.out.println(
      "Updated row in database by an amount that would lead to base targetValue, test can start"
    );
  }

  public void measure() {
    // CPU USAGE, DISK, MEMORY sampling start
    // Start timer of one second
    // Print number of responses seen in this time period
    // Stop the timer
    // CPU USAGE, DISK, MEMORY sampling end
  }

  public void teardown() {
    // Reset the database fields to 0,
    // Ensure the client sees these events before set up again
  }
}
