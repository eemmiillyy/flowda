package flow.core.Errors;

/*
 * 4000
 * "Client error. Unable to parse body of request. Needs to be JSON."
 */
public class ClientErrorJsonParseError extends ErrorBase {

  public ClientErrorJsonParseError() {
    this.code = 4000;
    this.message =
      "Client error. Unable to parse body of request. Needs to be JSON";
  }
}
