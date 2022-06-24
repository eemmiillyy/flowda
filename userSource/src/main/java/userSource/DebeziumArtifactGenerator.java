package userSource;

import java.net.URI;

/**
 * Takes a connection string and returns a kafka formatted connector string
 */
public class DebeziumArtifactGenerator {

  public String connectionString(
    String connectionString,
    String environmentId
  ) {
    // URI uri = URI.create("mysql://mysqluser:mysqlpw@127.0.0.1:3306/inventory");
    URI uri = URI.create(connectionString);
    String dbServerName = environmentId;
    String connectorName = dbServerName + "-connector";
    String[] userInfo = uri.getUserInfo().split(":");

    String host = uri.getHost();
    Number port = uri.getPort();
    String dbName = uri.getPath().substring(1); // Remove / at beginning of db name
    String username = userInfo[0];
    String password = userInfo[1];

    String formatted = String.format(
      "{ \"name\": \"%s\", \"config\": { \"connector.class\": \"io.debezium.connector.mysql.MySqlConnector\", \"tasks.max\": \"1\", \"database.hostname\": \"%s\", \"database.port\": \"%s\", \"database.user\": \"%s\", \"database.password\": \"%s\", \"database.server.id\": \"184054\", \"database.server.name\": \"%s\", \"database.include.list\": \"%s\", \"database.history.kafka.bootstrap.servers\": \"kafka:9092\", \"database.history.kafka.topic\": \"dbhistory.%s\" } }",
      connectorName,
      host,
      port,
      username,
      password,
      dbServerName,
      dbName,
      dbName
    );

    return formatted;
  }
}
