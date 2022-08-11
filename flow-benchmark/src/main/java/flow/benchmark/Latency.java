package flow.benchmark;

import java.util.Arrays;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import flow.benchmark.utils.KafkaClient;
import flow.benchmark.utils.MysqlClient;

public class Latency extends BenchmarkTestBase {

  public int targetValue;
  public KafkaConsumer<String, String> client;
  public MysqlClient mysqlClient;
  protected String environmentId;
  String testType = "";
  String resetQuery;
  String updateQuery;
  String kafkaTopic;
  String matcher;
  String connectionString;
  String user;
  String pass;

  public Latency(
    String environmentId,
    String resetQuery,
    String updateQuery,
    int iterations,
    int warmups,
    String testType,
    String matcher,
    String connectionString,
    String user,
    String pass
  )
    throws Exception {
    this.environmentId = environmentId;
    this.testType = testType;
    this.resetQuery = resetQuery;
    this.updateQuery = updateQuery;
    this.kafkaTopic =
      this.environmentId + ".inventory.custom_output_table_name";
    this.matcher = matcher;
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
    this.targetValue =
      Math.abs((new Long(System.currentTimeMillis()).intValue() % 10000));
    KafkaClient constructor = new KafkaClient();
    try {
      client = constructor.create(this.environmentId);
      client.subscribe(Arrays.asList(this.kafkaTopic));
    } catch (Exception e) {
      System.out.println("Issue with Latency setup" + e);
    }
    mysqlClient = new MysqlClient(this.connectionString, this.user, this.pass);
    mysqlClient.runQuery(this.resetQuery);
    mysqlClient.runQuery(String.format(this.updateQuery, targetValue));
  }

  public void measure(int i) {
    System.out.println(".................measuring...............");
    // Print the time when the fx starts and fx ends
    long startTime = System.currentTimeMillis();
    Boolean matched = false;
    while (matched == false) {
      ConsumerRecords<String, String> records = client.poll(100);
      for (ConsumerRecord<String, String> record : records) {
        if (
          record.value().equals(String.format(this.matcher, this.targetValue))
        ) {
          System.out.println(
            ".................found matching record..............."
          );
          System.out.println(record.value());
          matched = true; // Should be only one match
          break;
        }
      }
    }
    long endTime = System.currentTimeMillis();
    long elapsedTime = endTime - startTime;
    System.out.println("elapsedTime to find record:" + elapsedTime);
    client.close();
    // TODO CPU USAGE, DISK, MEMORY sampling end
  }

  public void teardown() throws Exception {
    mysqlClient.runQuery(this.resetQuery);
  }
}
