package flow.core.Errors;

public class ServerErrorBadConnection extends ErrorBase {

  public ServerErrorBadConnection() {
    this.code = 4008;
    this.message =
      "Internal server error. Issue connecting to the database. Bad connection string or privileges";
  }
}
