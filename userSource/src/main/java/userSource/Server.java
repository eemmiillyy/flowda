package userSource;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Server {

  public static void main(String[] strings)
    throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidParameterSpecException, InvalidKeyException, IOException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
    // Bootstrap bootstrap = new Bootstrap();
    // bootstrap.start();
    // System.out.println("Number of threads in main" + Thread.activeCount());
    Settings settings = new Settings();
    settings.load();
    SettingsShapeEncrypted s = Settings.settings;
    String kafkaAdminPassword =
      s.stage.production.services.kafka.admin.$$password;
    String kafkaAdminUser = s.stage.production.services.kafka.admin.user;
    System.out.println(kafkaAdminUser);
    System.out.println(settings.decryptField(kafkaAdminPassword));
  }
}
