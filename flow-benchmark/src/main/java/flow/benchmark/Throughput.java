package flow.benchmark;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import flow.benchmark.utils.KafkaClient;
import flow.benchmark.utils.MysqlClient;

public class Throughput extends BenchmarkTestBase {

  public int iterations = 0;
  public int warmups = 0;
  public KafkaConsumer<String, String> client;
  public MysqlClient mysqlClient;
  protected flow.benchmark.utils.MysqlClient.seedSerial runnable;
  Thread thread;
  protected String environmentId = "jajajaja";

  public Throughput() throws Exception {
    // Double the iterations for a warm up period.
    for (int i = 0; i <= iterations + warmups; i++) {
      setup();
      measure();
      teardown();
    }
  }

  public void setup() throws Exception {
    // Create kafka client listening to topic
    KafkaClient constructor = new KafkaClient();
    try {
      client = constructor.create(this.environmentId);
      client.subscribe(
        Arrays.asList(
          this.environmentId + ".inventory.custom_output_table_name"
        )
      );
    } catch (Exception e) {
      System.out.println("Issue with Latency setup" + e);
    }
    // Reset to zero
    mysqlClient = new MysqlClient();
    mysqlClient.runQuery(
      "UPDATE products_on_hand SET quantity=0 WHERE !isnull (product_id);"
    );
    this.runnable = mysqlClient.new seedSerial(mysqlClient);
    this.thread = new Thread(runnable);
    this.thread.start();

    System.out.println("Not waiting for this serial to complete");
  }

  // seed -------------------- 3s
  // wait half a second to account for latency,
  // how many records have been seeded at 0,5s? how many at 1.5seconds?
  // measure 0.5 seconds later ----- for 1s, so poll 1.5 seconds later
  // and see how many records came through.
  public void measure() throws Exception {
    // Wait one minute then poll, adjusted to average latency per message
    try {
      TimeUnit.MILLISECONDS.sleep(1440);
    } catch (InterruptedException e) {
      System.out.println("Issue sleeping");
    }
    PrintWriter writer = new PrintWriter(
      "src/main/resources/output.txt",
      "UTF-8"
    );

    ConsumerRecords<String, String> records = client.poll(2000);
    for (ConsumerRecord<String, String> record : records) {
      System.out.println("Record key size:: " + record.serializedKeySize());
      System.out.println("Record value size::" + record.serializedValueSize());
      writer.println(record.value());
    }
    System.out.println("done");

    client.close();
    writer.close();
    runnable.kill();
    thread.interrupt();
  }

  public void teardown() {}
}
