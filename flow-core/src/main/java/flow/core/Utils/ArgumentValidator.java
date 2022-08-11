package flow.core.Utils;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import flow.core.Settings.Settings;

public class ArgumentValidator {

  public class ValidationError extends RuntimeException {

    public ValidationError(String arg) {
      super(arg);
    }
  }

  public final int MAX_STRING_LENGTH = 256;
  private Settings settings;

  public ArgumentValidator(Settings settings) {
    this.settings = settings;
  }

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
      "^[a-zA-Z0-9-_(),'.= ]*$"
    );
    Matcher matcher = alphanumericWithSpecialCharacters.matcher(field);
    if (!matcher.matches()) {
      throw new ValidationError(
        String.format(
          "VALIDATION EXCEPTION: %s must be alpha numeric characters only, as well as (),_-.'",
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

  /**
   * The environment id is the same user for kafka,
   * and we want to make sure they cannot escalate their privileges
   * by submitting the same environment id as the admin user from settings.
   */
  public boolean validateEnvironmentId(String environmentId) {
    Pattern rootUserName = Pattern.compile(
      String.format("^[%s]*$", this.settings.settings.services.kafka.admin.user)
    );
    Matcher matcher = rootUserName.matcher(environmentId);
    if (matcher.matches()) {
      // Don't want to give away too much information about the username
      throw new ValidationError(
        String.format(
          "VALIDATION EXCEPTION:Environment id %s is taken.",
          environmentId
        )
      );
    }
    return true;
  }
}
