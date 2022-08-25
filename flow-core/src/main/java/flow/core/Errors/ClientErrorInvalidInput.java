package flow.core.Errors;

/*
 * 4002
 * "Client error. Invalid user input."
 */
public class ClientErrorInvalidInput extends ErrorBase {

  public ClientErrorInvalidInput() {
    this.code = 4002;
    this.message = "Client error. Invalid user input";
  }
}
