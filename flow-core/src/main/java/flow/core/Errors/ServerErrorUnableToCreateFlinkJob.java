package flow.core.Errors;

/**
 * 4007
 * "Internal server error. Unable to create job with Flink."
 */
public class ServerErrorUnableToCreateFlinkJob extends ErrorBase {

  public ServerErrorUnableToCreateFlinkJob() {
    this.code = 4007;
    this.message = "Internal server error. Unable to create job with Flink";
  }
}
