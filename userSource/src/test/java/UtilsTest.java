import static org.junit.jupiter.api.Assertions.assertEquals;

import java.security.GeneralSecurityException;

import userSource.Utils.ApiKey;

public class UtilsTest {

  public void testICanGenerateAnAPIKey() throws GeneralSecurityException {
    ApiKey apikeyFactory = new ApiKey();
    String apiKey = apikeyFactory.create();
    assertEquals(apiKey.length(), 44);
    // Base64 encoding adds ~33% increase to original source (32 bytes)
  }
}
