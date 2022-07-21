package flow.core.Utils;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArgumentValidator {

  public class ValidationError extends RuntimeException {

    public ValidationError(String arg) {
      super(arg);
    }
  }

  public final int MAX_STRING_LENGTH = 256;

  public boolean validateConnectionString(String connectionString)
    throws ValidationError {
    if (connectionString.length() > MAX_STRING_LENGTH) {
      throw new ValidationError(
        "VALIDATION EXCEPTION: Connection string must be under 256 characters"
      );
    }

    Pattern protocolPrefix = Pattern.compile("^mysql+:.*");
    Matcher matcher = protocolPrefix.matcher(connectionString);
    if (!matcher.matches()) {
      throw new ValidationError(
        "VALIDATION EXCEPTION: Connection string does not use a valid mysql protocol. A valid protocol looks like: mysql://..."
      );
    }

    try {
      URI.create(connectionString);
    } catch (IllegalArgumentException e) {
      throw new ValidationError(
        "VALIDATION EXCEPTION: Connection string is not in a valid format. A valid format looks like: mysql://user:pass@host:port/dbname"
      );
    }
    return true;
  }

  public boolean validateStringInput(String field, String fieldName)
    throws ValidationError {
    Pattern alphanumericWithSpecialCharacters = Pattern.compile(
      "^[a-zA-Z0-9-_(),'. ]*$"
    );
    Matcher matcher = alphanumericWithSpecialCharacters.matcher(field);
    if (!matcher.matches()) {
      throw new ValidationError(
        String.format(
          "VALIDATION EXCEPTION: %s must be alpha numeric characters only",
          fieldName
        )
      );
    }
    if (field.length() > MAX_STRING_LENGTH) {
      throw new ValidationError(
        String.format(
          "VALIDATION EXCEPTION: %s must be under 256 characters",
          fieldName
        )
      );
    }
    return true;
  }
}
