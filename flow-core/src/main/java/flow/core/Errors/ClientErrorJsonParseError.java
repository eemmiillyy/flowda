package flow.core.Errors;

public class ClientErrorJsonParseError extends ErrorBase {

  public ClientErrorJsonParseError() {
    this.code = 4000;
    this.message =
      "Client error. Unable to parse body of request. Needs to be JSON";
  }
}
