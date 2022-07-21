package userSource.Connector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectorResponseType {

  public String name;

  public Config config;
  public String error_code;
  public String message;

  @JsonIgnoreProperties(ignoreUnknown = true)
  public class Config {

    public String name;
  }
}
