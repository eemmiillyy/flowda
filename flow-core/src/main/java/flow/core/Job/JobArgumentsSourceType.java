package flow.core.Job;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JobArgumentsSourceType extends JobArgumentsBaseType {

  @JsonProperty("debezium-json.schema-include")
  public String debezium$json_schema$include = "true";
}
