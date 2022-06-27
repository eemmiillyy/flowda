package userSource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * 
 * Given this payload ddl from the server kafka topic after connecting with debezium.
 * Sub the create statement input datatype.
 * 
 * @param model an array of the models to find
 * @return       the transformed prisma schema into a kafka connect acceptable format.  
 * 
 * https://nightlies.apache.org/flink/flink-docs-release-1.11/dev/table/types.html
 * https://nightlies.apache.org/flink/flink-docs-release-1.11/dev/table/sql/create.html
 * https://www.prisma.io/docs/concepts/database-connectors/mysql
 * 
 * e.g. 
 * 
 * Input:
 * CREATE TABLE `customers` (  `id` int NOT NULL AUTO_INCREMENT,  `first_name` varchar(255) NOT NULL,  `last_name` varchar(255) NOT NULL,  `email` varchar(255) NOT NULL,  PRIMARY KEY (`id`),  UNIQUE KEY `email` (`email`)) ENGINE=InnoDB AUTO_INCREMENT=1005 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
 * 
 * Output:
 * 
 * CREATE TABLE `customers` 
 * (  `id` int,
 *  `first_name` varchar,
 * `last_name` varchar,
 *  `email` varchar,
 * "event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, " 
 * "WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND " +
"
 */

public class FlinkArtifactGenerator {

  public class Columns {

    String name;
    String typeName;
  }

  public class Table {

    Columns[] columns;
  }

  public class TableChanges {

    String type;
    String id;
    Table table;
  }

  public class Source {

    String table;
    String name;
  }

  public class Payload {

    Source source;
    TableChanges[] tableChanges;
    String databaseName;
  }

  public class DataObject {

    Payload payload;
  }

  KafkaClient kafka;

  public FlinkArtifactGenerator(KafkaClient client) {
    this.kafka = client;
  }

  public String createAgreggateQuery(String tableName, String fieldName) {
    return String.format(
      "SELECT SUM(%s) as summed FROM %s;",
      fieldName,
      tableName
    );
  }

  // TODO Reuse logic for source and sink config
  public String createSinkTable(
    String environmentId,
    String databaseName,
    String tableName,
    String fieldName
  ) {
    return (
      "CREATE TABLE " +
      tableName +
      "_output (" +
      "summed INT" +
      ") WITH (" +
      "'connector' = 'kafka'," +
      "'topic' = '" +
      environmentId +
      "." +
      databaseName +
      "." +
      tableName +
      "_output" +
      "'," +
      "'properties.bootstrap.servers' = 'localhost:9093'," +
      " 'properties.sasl.mechanism' = 'SCRAM-SHA-256', " +
      " 'properties.security.protocol' = 'SASL_PLAINTEXT', " +
      " 'properties.sasl.jaas.config' = 'org.apache.kafka.common.security.scram.ScramLoginModule required username=emily password=bleepbloop;', " +
      "'format' = 'debezium-json'" +
      ")"
    );
  }

  /**
   *
   * @param environmentId doubles as the chosen database server name and kafka topic id. Should be what was passed in when creating original connection.
   * @param databaseName
   * @param tableName
   * @return The string necessary for a Flink Kafka source connector to be started.
   * @throws Throwable
   */
  public String createSourceTable(
    String environmentId,
    String databaseName,
    String tableName
  )
    throws Throwable {
    System.out.println(environmentId);
    System.out.println(databaseName);
    System.out.println(tableName);
    // TODO this creation is blocking so should have it's own thread.
    // One Consumer Per Thread
    KafkaConsumer<String, String> client = kafka.create();

    // Check if topic exists
    Map<String, List<PartitionInfo>> topicMap = client.listTopics();
    if (!(topicMap.containsKey(environmentId))) {
      throw new Exception("environmentId (topic name) is wrong");
    }

    // Subscribe to schema topic
    client.subscribe(Arrays.asList(environmentId));

    boolean matched = false;

    String output = "";

    String toMatch = "CREATE TABLE `" + tableName + "`";

    try {
      ConsumerRecords<String, String> records = client.poll(1000);
      for (ConsumerRecord<String, String> record : records) {
        if (record.value().contains(toMatch)) {
          output = record.value();
          matched = true; // Should be only one match
          break;
        }
      }
      if (!matched) {
        // No table in db with that name
        throw new Exception("table name is wrong");
      }
    } finally {
      client.close();
    }
    String sql = output;

    Gson g = new Gson();

    Map<String, String> listOfKeys = new HashMap<String, String>();

    output = "CREATE TABLE " + tableName + " (";

    try {
      DataObject obj = g.fromJson(sql, DataObject.class);

      if (Objects.equals(obj.payload.source.name, environmentId)) {
        if (Objects.equals(obj.payload.databaseName, databaseName)) {
          if (Objects.equals(obj.payload.source.table, tableName)) {
            if (obj.payload.tableChanges.length > 0) {
              if (Objects.equals(obj.payload.tableChanges[0].type, "CREATE")) {
                for (Columns col : obj.payload.tableChanges[0].table.columns) {
                  // Get all fields and field types for this table create statement
                  listOfKeys.put(col.name, col.typeName);
                }
              }
            }
          }
        } else {
          throw new Exception("database name is wrong");
        }
      }

      // Format the create statement
      for (Map.Entry<String, String> entry : listOfKeys.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();
        output += key + " " + value + ", ";
      }
      output +=
        "event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL,  " +
        "WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND " +
        ") WITH (" +
        " 'connector' = 'kafka'," +
        "'topic'     = '" +
        environmentId +
        "." +
        databaseName +
        "." +
        tableName +
        "'," +
        " 'properties.bootstrap.servers' = 'localhost:9093'," +
        " 'properties.group.id' = '1391083'," +
        " 'properties.sasl.mechanism' = 'SCRAM-SHA-256', " +
        " 'properties.security.protocol' = 'SASL_PLAINTEXT', " +
        " 'properties.sasl.jaas.config' = 'org.apache.kafka.common.security.scram.ScramLoginModule required username=emily password=bleepbloop;', " +
        " 'debezium-json.schema-include' = 'true', " +
        " 'scan.startup.mode' = 'earliest-offset'," +
        " 'format'    = 'debezium-json'" +
        ")";
      return output;
      // TODO cannot connect with these configs - need SCRAM specific config
    } catch (JsonSyntaxException e) {
      // THROW error?
      return e.getMessage();
    }
  }
}
