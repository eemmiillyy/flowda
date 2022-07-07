package userSource.Utils;

public class ArgumentValidator {

  public class ValidationError extends RuntimeException {

    public ValidationError(String arg) {
      super(arg);
    }
  }

  public boolean validateConnectionString(String connectionString)
    throws ValidationError {
    if (connectionString.length() > 256) {
      throw new ValidationError("VALIDATIONEXCEPTION");
    }
    // TODO validate characters

    return true;
  }
}
