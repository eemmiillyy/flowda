package userSource.Flink;

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

import userSource.Kafka.KafkaClient;
import userSource.Settings.Settings;

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

  private String loginModule;
  Settings settings;

  public FlinkArtifactGenerator(Settings settings) {
    this.settings = settings;

    this.loginModule =
      String.format(
        " 'properties.sasl.jaas.config' = 'org.apache.kafka.common.security.scram.ScramLoginModule required username=%s password=%s;', ",
        this.settings.settings.services.kafka.admin.user,
        settings.decryptField(
          this.settings.settings.services.kafka.admin.$$password
        )
      );
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
    String databaseName,
    String tableName,
    String environmentId
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
      "'properties.bootstrap.servers' = '" +
      this.settings.settings.services.kafka.bootstrap.serversExternal +
      "'," +
      " 'properties.sasl.mechanism' = '" +
      this.settings.settings.services.kafka.sasl.mechanism +
      "', " +
      " 'properties.security.protocol' = '" +
      this.settings.settings.services.kafka.sasl.protocol +
      "', " +
      this.loginModule +
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
  public KafkaConsumer<String, String> createKafkaConsumer() {
    KafkaClient kafka = new KafkaClient(
      this.settings.settings.services.kafka.admin.user,
      settings.decryptField(
        this.settings.settings.services.kafka.admin.$$password
      ),
      this.settings
    );
    KafkaConsumer<String, String> client = kafka.create(
      this.settings.settings.services.kafka.admin.user
    );
    return client;
  }

  public String createSourceTable(
    String databaseName,
    String tableName,
    String environmentId
  )
    throws Throwable {
    KafkaConsumer<String, String> client = createKafkaConsumer();
    Map<String, List<PartitionInfo>> topicMap;
    try {
      topicMap = client.listTopics();
    } catch (Exception e) {
      throw new Exception("Unable to connect to kafka cluster");
    }

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
        throw new Exception(
          "Could not find provided table name in kafka records. Please check that your table name is correct AND that the connection string provided is valid."
        );
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
          throw new Exception(
            "Provided database name does not match kafka records."
          );
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
        " 'properties.bootstrap.servers' = '" +
        this.settings.settings.services.kafka.bootstrap.serversExternal +
        "'," +
        " 'properties.group.id' = '" +
        environmentId +
        "'," +
        " 'properties.sasl.mechanism' = '" +
        this.settings.settings.services.kafka.sasl.mechanism +
        "', " +
        " 'properties.security.protocol' = '" +
        this.settings.settings.services.kafka.sasl.protocol +
        "', " +
        this.loginModule +
        " 'debezium-json.schema-include' = 'true', " +
        " 'scan.startup.mode' = 'earliest-offset'," +
        " 'format'    = 'debezium-json'" +
        ")";
      System.out.println(output);
      return output;
    } catch (JsonSyntaxException e) {
      // THROW error?
      return e.getMessage();
    }
  }
}
