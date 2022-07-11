package userSource.Flink;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FlinkResponseShape {

  public String jobid;
}
