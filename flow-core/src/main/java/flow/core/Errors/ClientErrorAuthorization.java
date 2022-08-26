package flow.core.Errors;

public class ClientErrorAuthorization extends ErrorBase {

  public ClientErrorAuthorization() {
    this.code = 4005;
    this.message = "Client error. Invalid authorization jwt header";
  }
}
