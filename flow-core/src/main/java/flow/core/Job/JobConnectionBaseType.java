package flow.core.Job;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JobConnectionBaseType {

  public String connector = "kafka";
  public String topic;

  @JsonProperty("properties.bootstrap.servers")
  public String properties_bootstrap_servers;

  @JsonProperty("scan.startup.mode")
  public String scan_startup_mode = "earliest-offset";

  @JsonProperty("format")
  public String format = "debezium-json";
}
