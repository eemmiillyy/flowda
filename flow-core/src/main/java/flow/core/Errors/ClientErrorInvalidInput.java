package flow.core.Errors;

public class ClientErrorInvalidInput extends ErrorBase {

  public ClientErrorInvalidInput() {
    this.code = 4002;
    this.message = "Client error. Invalid user input";
  }
}
