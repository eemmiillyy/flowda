package userSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;

public class Settings {

  // TODO encrypt needs to change the key to single dollar sign
  // TODO Decrypt needs to change the key to multi dollar sign
  // TODO inject env variable password

  Pattern doubleDollarSignPattern = Pattern.compile("^\\$\\$[A-Za-z0-9]+");
  // Pattern singleDollarSignPattern = Pattern.compile("^\\$[A-Za-z0-9]+");
  static SettingsShapeEncrypted settings;
  String password = "password"; // TODO take from .env

  byte[] salt;
  int interationCount;
  int keyLength;
  String alg;
  String transformation;
  String symmetricAlg;
  String charset;
  byte[] iv;
  SecretKeyFactory secretKey;
  SecretKey encryptedKey;
  Cipher cipher;

  public Settings()
    throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidParameterSpecException, InvalidKeyException, IOException {
    this.load();

    SecureRandom random = new SecureRandom();
    byte[] salt = new byte[16];
    random.nextBytes(salt);
    this.salt =
      new byte[] {
        -83,
        -12,
        -36,
        -98,
        126,
        21,
        28,
        -107,
        126,
        -68,
        56,
        -26,
        50,
        -12,
        -37,
        -50,
      };
    this.interationCount = 1000;
    this.keyLength = 256;
    this.alg = "PBKDF2WithHmacSHA1";
    this.transformation = "AES/CBC/PKCS5Padding";
    this.symmetricAlg = "AES";
    this.charset = "ASCII";

    KeySpec spec = new PBEKeySpec(
      password.toCharArray(),
      this.salt,
      this.interationCount,
      this.keyLength
    );
    this.secretKey = SecretKeyFactory.getInstance(this.alg);
    this.encryptedKey = secretKey.generateSecret(spec);
    this.cipher = Cipher.getInstance(this.transformation);
    this.cipher.init(
        Cipher.ENCRYPT_MODE,
        new SecretKeySpec(this.encryptedKey.getEncoded(), this.symmetricAlg)
      );
    this.iv =
      new byte[] {
        -82,
        -85,
        -93,
        -18,
        25,
        -41,
        -16,
        -87,
        5,
        98,
        20,
        -115,
        -17,
        90,
        -3,
        -71,
      };
    // this.cipher.getParameters()
    //   .getParameterSpec(IvParameterSpec.class)
    //   .getIV();

    /** After encryption, replace plaintext and save these and set the values to these for decyption */
    System.out.println(Arrays.toString(this.salt));
    System.out.println(Arrays.toString(this.iv));
  }

  /**
   * Load file into memory
   * File needs to be in /tmp/settings.json
   * This should be packaged with the jar file eventually (TODO).
   * <code>
   * SettingsShapeEncrypted settings = new Settings().load(); // OR add load to constructor.
   * settings.[field].[field] or
   * settings.decryptField(settings.[field].[field])
   * </code>
   * @throws IOException
   */
  public void load() throws IOException {
    String fileName =
      "/Users/emilymorgan/Desktop/pdpDataProjections/userSource/src/main/java/userSource/Settings.json";
    Path p = Paths.get(fileName);
    InputStream inputStream = Files.newInputStream(p);
    JsonReader reader = new JsonReader(new InputStreamReader(inputStream));
    reader.beginArray();
    while (reader.hasNext()) {
      SettingsShapeEncrypted s = new Gson()
      .fromJson(reader, SettingsShapeEncrypted.class);
      System.out.println(s.stage.development.services.kafka.admin.user);
      Settings.settings = s;
    }
    reader.endArray();
  }

  // TODO threadsafe
  public void encrypt() {
    JsonObject jsonObject = (JsonObject) new Gson()
    .toJsonTree(Settings.settings);
    traverse(jsonObject, true);
    System.out.print(jsonObject);
  }

  // TODO threadsafe
  public void decrypt() {
    JsonObject jsonObject = (JsonObject) new Gson()
    .toJsonTree(Settings.settings);
    traverse(jsonObject, false);
    System.out.print(jsonObject);
  }

  public String encryptField(JsonElement field) throws Exception {
    byte[] plain = field.getAsString().getBytes(this.charset);
    byte[] ciphertext = this.cipher.doFinal(plain);
    return new String(Base64.getEncoder().encode(ciphertext));
  }

  public String decryptField(JsonElement field)
    throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, InvalidParameterSpecException, InvalidKeyException, InvalidAlgorithmParameterException {
    byte[] plain = Base64.getDecoder().decode(field.getAsString());
    this.cipher.init(
        Cipher.DECRYPT_MODE,
        new SecretKeySpec(this.encryptedKey.getEncoded(), this.symmetricAlg),
        new IvParameterSpec(this.iv)
      );
    return new String(this.cipher.doFinal(plain), this.charset);
  }

  /**
   * <code>Gson g = new Gson();
    SettingsShapeEncrypted obj = g.fromJson(
      jsonObject,
      SettingsShapeEncrypted.class
    );
    decryptField(obj.stage.development.$$password);</code>
   * 
   */
  public String decryptField(String field)
    throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, InvalidParameterSpecException, InvalidKeyException, InvalidAlgorithmParameterException {
    byte[] plain = Base64.getDecoder().decode(field);
    this.cipher.init(
        Cipher.DECRYPT_MODE,
        new SecretKeySpec(this.encryptedKey.getEncoded(), this.symmetricAlg),
        new IvParameterSpec(this.iv)
      );
    return new String(this.cipher.doFinal(plain), this.charset);
  }

  /**
   * Goes through the all children nodes of the element passed in until
   * it hits a primary field.
   * @param jsonElement Starting elemenet.
   * @param encrypt Boolean - if true it encrypts all subsequet fields, otherwise decrypts.
   */
  public void traverse(JsonElement jsonElement, Boolean encrypt) {
    JsonObject jsonObject = (JsonObject) jsonElement;

    Set<Map.Entry<String, JsonElement>> entries = jsonObject.entrySet();
    for (Map.Entry<String, JsonElement> entry : entries) {
      if (entry.getValue() instanceof JsonPrimitive) {
        // check if field has dollar sign and needs to be encrypted
        Matcher matcher = encrypt
          ? this.doubleDollarSignPattern.matcher(entry.getKey())
          : this.doubleDollarSignPattern.matcher(entry.getKey());
        if (matcher.matches()) {
          JsonObject replaceWith = new JsonObject();
          try {
            replaceWith.addProperty(
              entry.getKey(),
              encrypt
                ? encryptField(entry.getValue())
                : decryptField(entry.getValue())
            );
          } catch (Exception e) {
            // TODO Auto-generated catch block
            System.out.println("...ERROR OCCURRED");
            e.printStackTrace();
          }
          // Encrypt value
          entry.setValue(replaceWith.get(entry.getKey()));
        }
      } else if (entry.getValue() instanceof JsonObject) {
        traverse(entry.getValue(), encrypt);
      } else {
        // null value
        return;
      }
    }
  }
}
