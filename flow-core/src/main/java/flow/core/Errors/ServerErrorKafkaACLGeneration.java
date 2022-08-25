package flow.core.Errors;

/**
 * 4004
 * "Internal server error. Issue generating Kafka ACL rule."
 */
public class ServerErrorKafkaACLGeneration extends ErrorBase {

  public ServerErrorKafkaACLGeneration() {
    this.code = 4004;
    this.message = "Internal server error. Issue generating Kafka ACL rule";
  }
}
