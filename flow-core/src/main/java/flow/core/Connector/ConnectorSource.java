package flow.core.Connector;

import flow.core.Settings.Settings;
import flow.core.Settings.SettingsType.Stage.StageInstance;
import flow.core.Utils.ConnectionStringParser;
import flow.core.Utils.ConnectionStringParser.ConnectionStringParsed;

/**
 * Takes a connection string and returns a kafka formatted connector string
 */
public class ConnectorSource {

  Settings settings;

  public ConnectorSource(Settings settings) {
    this.settings = settings;
  }

  public String connectionString(
    String connectionString,
    String environmentId
  ) {
    String dbServerName = environmentId;
    String connectorName = dbServerName + "-connector";

    ConnectionStringParsed connectionInfo = new ConnectionStringParser()
    .parse(connectionString);

    StageInstance stage = this.settings.settings;

    String formatted = String.format(
      "{ \"name\": \"%s\", \"config\": { \"connector.class\": \"io.debezium.connector.mysql.MySqlConnector\", \"tasks.max\": \"1\", \"database.hostname\": \"%s\", \"database.port\": \"%s\", \"database.user\": \"%s\", \"database.password\": \"%s\", \"database.server.id\": \"184054\", \"database.server.name\": \"%s\", \"database.include.list\": \"%s\", \"database.history.kafka.bootstrap.servers\": \"%s\", \"database.history.kafka.topic\": \"dbhistory.%s\" } }",
      connectorName,
      connectionInfo.host,
      connectionInfo.port,
      connectionInfo.username,
      connectionInfo.password,
      dbServerName,
      connectionInfo.dbName,
      stage.services.kafka.bootstrap.serversInternal,
      connectionInfo.dbName
    );

    return formatted;
  }
}
