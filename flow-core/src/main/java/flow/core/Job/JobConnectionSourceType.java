package flow.core.Job;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JobConnectionSourceType extends JobConnectionBaseType {

  @JsonProperty("debezium-json.schema-include")
  public String debezium$json_schema$include = "true";
}
