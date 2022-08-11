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

  public KafkaConsumer<String, String> client;
  public MysqlClient mysqlClient;
  protected String environmentId;
  protected flow.benchmark.utils.MysqlClient.seedSerial runnable;
  Thread thread;
  int sleepMs = 1440;
  String testType = "";
  String resetQuery;
  String updateQuery;
  String kafkaTopic;
  int warmups;
  String connectionString;
  String user;
  String pass;

  public Throughput(
    String environmentId,
    String resetQuery,
    String updateQuery,
    int iterations,
    int warmups,
    String testType,
    String connectionString,
    String user,
    String pass
  )
    throws Exception {
    this.environmentId = environmentId;
    this.testType = testType;
    this.resetQuery = resetQuery;
    this.updateQuery = updateQuery;
    this.warmups = warmups;
    this.kafkaTopic =
      this.environmentId + ".inventory.custom_output_table_name";
    this.connectionString = connectionString;
    this.user = user;
    this.pass = pass;

    for (int i = 0; i <= iterations + warmups; i++) {
      setup();
      measure(i);
      teardown();
    }
  }

  public void setup() throws Exception {
    KafkaClient constructor = new KafkaClient();
    try {
      this.client = constructor.create(this.environmentId);
      this.client.subscribe(Arrays.asList(this.kafkaTopic));
    } catch (Exception e) {
      System.out.println("Issue with Latency setup simple" + e);
    }
    mysqlClient = new MysqlClient(this.connectionString, this.user, this.pass);
    mysqlClient.runQuery(this.resetQuery);
    this.runnable = mysqlClient.new seedSerial(mysqlClient, this.updateQuery);
    this.thread = new Thread(runnable);
    this.thread.start();
  }

  public void measure(int iteration) throws Exception {
    // Wait one minute then poll, adjusted to average latency per message
    try {
      TimeUnit.MILLISECONDS.sleep(this.sleepMs);
    } catch (InterruptedException e) {
      System.out.println("Issue sleeping");
    }

    if (iteration > this.warmups) {
      PrintWriter writer = new PrintWriter(
        String.format(
          "src/main/resources/output%s-%s.txt",
          this.warmups + this.iterations - iteration,
          this.testType
        ),
        "UTF-8"
      );

      ConsumerRecords<String, String> records = this.client.poll(2000);
      for (ConsumerRecord<String, String> record : records) {
        System.out.println(
          "Record value size::" + record.serializedValueSize()
        );
        writer.println(record.value());
      }
      System.out.println("done");

      writer.close();
    } else {
      // Make sure record does not show up in consumer poll so results are easier to read
      ConsumerRecords<String, String> records = this.client.poll(2000);
      for (ConsumerRecord<String, String> record : records) {
        System.out.println(
          "Record value size::" + record.serializedValueSize()
        );
      }
    }

    this.client.close();
    runnable.kill();
    thread.interrupt();
  }

  public void teardown() {}
}
