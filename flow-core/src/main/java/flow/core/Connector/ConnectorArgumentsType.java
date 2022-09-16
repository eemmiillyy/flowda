package flow.core.Connector;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConnectorArgumentsType {

  public String name;
  public Config config;

  public class Config {

    @JsonProperty("connector.class")
    public String connector_class =
      "io.debezium.connector.mysql.MySqlConnector";

    @JsonProperty("tasks.max")
    public String tasks_max = "1";

    @JsonProperty("database.hostname")
    public String database_hostname;

    @JsonProperty("database.port")
    public String database_port;

    @JsonProperty("database.user")
    public String database_user;

    @JsonProperty("database.password")
    public String database_password;

    @JsonProperty("database.server.id")
    public String database_server_id = "184054";

    @JsonProperty("database.server.name")
    public String database_server_name;

    @JsonProperty("database.include.list")
    public String database_include_list;

    @JsonProperty("database.history.kafka.bootstrap.servers")
    public String database_history_kafka_bootstrap_servers;

    @JsonProperty("database.history.kafka.topic")
    public String database_history_kafka_topic;
  }
}
