package userSource;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;

import javax.crypto.NoSuchPaddingException;

public class Server {

  public static void main(String[] strings)
    throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidParameterSpecException, InvalidKeyException {
    // Bootstrap bootstrap = new Bootstrap();
    // bootstrap.start();
    // System.out.println("Number of threads in main" + Thread.activeCount());
    Settings settings = new Settings();
    settings.encrypt();
    System.out.println("DECRYPTING NOW....");
    settings.decrypt();
  }
}
