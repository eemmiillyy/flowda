package flow.core.Connector;

import com.google.gson.Gson;

import flow.core.Settings.Settings;
import flow.core.Utils.ClassToStringConverter;
import flow.core.Utils.ConnectionStringParser;
import flow.core.Utils.ConnectionStringParser.ConnectionStringParsed;

/**
 * Takes a connection string and returns a kafka formatted connector string
 */
public class ConnectorSource {

  private Settings settings;
  private Gson g;

  public ConnectorSource(Settings settings) {
    this.settings = settings;
    this.g = new Gson();
  }

  public String build(String connectionString, String environmentId)
    throws Exception {
    String dbServerName = environmentId;
    String connectorName = dbServerName + "-connector";

    ConnectionStringParsed connectionInfo = new ConnectionStringParser()
    .parse(connectionString);

    ConnectorConnectionType connectionObject =
      this.g.fromJson("{}", ConnectorConnectionType.class);

    connectionObject.config = connectionObject.new Config();

    connectionObject.name = connectorName;
    connectionObject.config.database_hostname = connectionInfo.host;
    connectionObject.config.database_port = connectionInfo.port;
    connectionObject.config.database_user = connectionInfo.username;
    connectionObject.config.database_password = connectionInfo.password;
    connectionObject.config.database_server_name = dbServerName;
    connectionObject.config.database_include_list = connectionInfo.dbName;
    connectionObject.config.database_history_kafka_bootstrap_servers =
      this.settings.settings.services.kafka.bootstrap.serversInternal;
    connectionObject.config.database_history_kafka_topic =
      "dbhistory." + connectionInfo.dbName;

    String connectorInfo = new ClassToStringConverter()
    .convertToJsonFormattedString(connectionObject, new StringBuilder());

    return ("{" + connectorInfo + "}");
  }
}
