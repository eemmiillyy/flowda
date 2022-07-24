package flow.core.Job;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JobConnectionBaseType {

  public String connector = "kafka";
  public String topic;

  @JsonProperty("properties.bootstrap.servers")
  public String properties_bootstrap_servers;

  @JsonProperty("properties.group.id")
  public String properties_group_id;

  @JsonProperty("properties.sasl.mechanism")
  public String properties_sasl_mechanism;

  @JsonProperty("properties.security.protocol")
  public String properties_security_protocol;

  @JsonProperty("properties.sasl.jaas.config")
  public String properties_sasl_jaas_config;

  @JsonProperty("format")
  public String format = "debezium-json";
}
