package flow.core.Errors;

/**
 * 4005
 * "Client error. Invalid authorization jwt header."
 */
public class ClientErrorAuthorization extends ErrorBase {

  public ClientErrorAuthorization() {
    this.code = 4005;
    this.message = "Client error. Invalid authorization jwt header";
  }
}
