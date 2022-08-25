package flow.core.Errors;

/**
 * 4001
 * "Client error. Missing user input."
 */
public class ClientErrorMissingInput extends ErrorBase {

  public ClientErrorMissingInput() {
    this.code = 4001;
    this.message = "Client error. Missing user input";
  }
}
