package userSource.Debezium;

import java.net.URI;

import userSource.Settings.Settings;
import userSource.Settings.SettingsShape.Stage.StageInstance;

/**
 * Takes a connection string and returns a kafka formatted connector string
 */
public class DebeziumArtifactGenerator {

  Settings settings;

  public DebeziumArtifactGenerator(Settings settings) {
    this.settings = settings;
  }

  public String connectionString(URI uri, String environmentId) {
    String dbServerName = environmentId;
    String connectorName = dbServerName + "-connector";
    String[] userInfo = uri.getUserInfo().split(":");

    String host = uri.getHost();
    Number port = uri.getPort();
    String dbName = uri.getPath().substring(1); // Remove / at beginning of db name
    String username = userInfo[0];
    String password = userInfo[1];

    StageInstance stage = this.settings.settings;

    String formatted = String.format(
      "{ \"name\": \"%s\", \"config\": { \"connector.class\": \"io.debezium.connector.mysql.MySqlConnector\", \"tasks.max\": \"1\", \"database.hostname\": \"%s\", \"database.port\": \"%s\", \"database.user\": \"%s\", \"database.password\": \"%s\", \"database.server.id\": \"184054\", \"database.server.name\": \"%s\", \"database.include.list\": \"%s\", \"database.history.kafka.bootstrap.servers\": \"%s\", \"database.history.kafka.topic\": \"dbhistory.%s\" } }",
      connectorName,
      host,
      port,
      username,
      password,
      dbServerName,
      dbName,
      stage.services.kafka.bootstrap.serversInternal,
      dbName
    );

    return formatted;
  }
}
