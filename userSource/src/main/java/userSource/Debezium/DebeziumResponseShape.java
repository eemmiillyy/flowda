package userSource.Debezium;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DebeziumResponseShape {

  public String name;

  public Config config;
  public String error_code;
  public String message;

  @JsonIgnoreProperties(ignoreUnknown = true)
  public class Config {

    public String name;
  }
}
