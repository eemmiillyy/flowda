package userSource;

import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Properties;

import org.apache.commons.crypto.random.CryptoRandom;
import org.apache.commons.crypto.random.CryptoRandomFactory;

public class ApiKey {

  public String create() throws GeneralSecurityException {
    byte[] randomBytes = new byte[32]; // 256 bit key

    Properties properties = new Properties();
    properties.put(
      CryptoRandomFactory.CLASSES_KEY,
      CryptoRandomFactory.RandomProvider.OS.getClassName()
    );

    CryptoRandom cryptoRandom = CryptoRandomFactory.getCryptoRandom(properties);
    // Show the actual class (may be different from the one requested)
    System.out.println(cryptoRandom.getClass().getCanonicalName());

    cryptoRandom.nextBytes(randomBytes);

    return new String(Base64.getEncoder().encode(randomBytes));
  }

  // TODO
  public void delete() {}
}
