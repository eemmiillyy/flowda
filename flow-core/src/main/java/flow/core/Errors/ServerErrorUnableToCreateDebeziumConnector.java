package flow.core.Errors;

public class ServerErrorUnableToCreateDebeziumConnector extends ErrorBase {

  public ServerErrorUnableToCreateDebeziumConnector() {
    this.code = 4003;
    this.message =
      "Internal server error. Unable to create connector with debezium";
  }
}
