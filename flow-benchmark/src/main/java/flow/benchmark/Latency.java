package flow.benchmark;

import java.util.Arrays;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import flow.benchmark.utils.KafkaClient;
import flow.benchmark.utils.MysqlClient;

public class Latency extends BenchmarkTestBase {

  public int iterations = 5;
  public int warmups = 15;
  public int targetValue;
  public KafkaConsumer<String, String> client;
  public MysqlClient mysqlClient;

  public Latency() throws Exception {
    // Double the iterations for a warm up period.
    for (int i = 0; i <= iterations + warmups; i++) {
      setup();
      measure();
      teardown();
    }
  }

  // TODO instead of sending one request,
  // send a bunch and monitor for the single one
  // with target value. When the 500th has sent - wait for a response, start measuring
  // until that value is seen
  // TODO Set timeout
  public void setup() throws Exception {
    targetValue = (new Long(System.currentTimeMillis()).intValue() % 10000);
    KafkaClient constructor = new KafkaClient();
    try {
      client = constructor.create();
      client.subscribe(
        Arrays.asList("emilytwo.inventory.custom_output_table_name")
      );
    } catch (Exception e) {
      System.out.println("Issue with Latency setup" + e);
    }
    mysqlClient = new MysqlClient();
    mysqlClient.runQuery(
      "UPDATE products_on_hand SET quantity=0 WHERE !isnull (product_id);"
    );

    // Update single row without waiting for response
    mysqlClient.runQuery(
      String.format(
        "UPDATE products_on_hand    SET quantity=%s   WHERE product_id=109;",
        targetValue
      )
    );
  }

  public void measure() {
    // CPU USAGE, DISK, MEMORY sampling start
    // Print the time when the fx starts and fx ends
    long startTime = System.currentTimeMillis();
    Boolean matched = false;
    while (matched == false) {
      ConsumerRecords<String, String> records = client.poll(100);
      for (ConsumerRecord<String, String> record : records) {
        // System.out.println(record.value());
        if (
          record
            .value()
            .equals(
              String.format(
                "{\"before\":null,\"after\":{\"summed\":%s},\"op\":\"c\"}",
                targetValue
              )
            )
        ) {
          matched = true; // Should be only one match
          break;
        }
      }
    }
    long endTime = System.currentTimeMillis();
    long elapsedTime = endTime - startTime;
    System.out.println(elapsedTime);
    client.close();
    // CPU USAGE, DISK, MEMORY sampling end
  }

  public void teardown() throws Exception {
    // Reset the database fields to 0
    // Ensure the client sees these events before set up again
    mysqlClient.runQuery(
      "UPDATE products_on_hand SET quantity=0 WHERE !isnull (product_id);"
    );
  }
}
