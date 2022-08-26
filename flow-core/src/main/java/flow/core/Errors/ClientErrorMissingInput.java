package flow.core.Errors;

public class ClientErrorMissingInput extends ErrorBase {

  public ClientErrorMissingInput() {
    this.code = 4001;
    this.message = "Client error. Missing user input";
  }
}
