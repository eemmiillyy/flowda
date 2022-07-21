package userSource.Job;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

public class JobSource {

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

    JobSource source;
    TableChanges[] tableChanges;
    String databaseName;
  }

  public class DataObject {

    Payload payload;
  }

  private String loginModule;
  Settings settings;

  public JobSource(Settings settings) {
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

  public String extractTableNameFromCreateStatement(String sourceSql) {
    String createStatement = "CREATE TABLE";
    Pattern pattern = Pattern.compile(createStatement + "\\W+(\\w+)");
    Matcher matcher = pattern.matcher(sourceSql);
    return matcher.find() ? matcher.group(1) : null;
  }

  public String appendKafkaConnectionInfo(
    Boolean omitDebeziumConfig,
    String content,
    String databaseName,
    String environmentId,
    String tableName
  ) {
    String output =
      content +
      "WITH (" +
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
      (omitDebeziumConfig ? "" : " 'debezium-json.schema-include' = 'true', ") +
      " 'scan.startup.mode' = 'earliest-offset'," +
      " 'format'    = 'debezium-json'" +
      ")";
    return output;
  }
}
