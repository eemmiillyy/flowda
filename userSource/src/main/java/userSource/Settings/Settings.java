package userSource.Settings;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
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

import userSource.Settings.SettingsShape.Stage.StageInstance;

public class Settings {

  // TODO inject env variable password

  Pattern doubleDollarSignPattern = Pattern.compile("^\\$\\$[A-Za-z0-9]+");

  public StageInstance settings;
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

  public Settings(String stage) {
    try {
      this.settings = this.load(stage, "src/main/resources/Settings.json");

      String saltBase64Encoded = "yGfJMijTbVzzx1Ywb3d2dd=="; // TODO take from .env
      this.salt = Base64.getDecoder().decode(saltBase64Encoded);

      String ivBase64Encoded = "J9eVrf/IcFnsjz9K4AQh/8=="; // TODO take from .env
      this.iv = Base64.getDecoder().decode(ivBase64Encoded);

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
          new SecretKeySpec(this.encryptedKey.getEncoded(), this.symmetricAlg),
          new IvParameterSpec(this.iv)
        );
    } catch (Throwable e) {
      System.out.println("COULD NOT LOAD SETTINGS FROM SETTINGS.JSON. ABORT.");
    }
  }

  /**
   * Load file into memory. Automatically called in contsructor.
   * @throws IOException
   */
  public SettingsShape.Stage.StageInstance load(String stage, String fileName)
    throws IOException {
    Path p = Paths.get(fileName);
    InputStream inputStream = Files.newInputStream(p);
    JsonReader reader = new JsonReader(new InputStreamReader(inputStream));

    reader.beginArray();
    while (reader.hasNext()) {
      SettingsShape s = new Gson().fromJson(reader, SettingsShape.class);
      System.out.println(stage + " from settings");
      return stage == "production"
        ? s.stage.production
        : stage == "test" ? s.stage.test : s.stage.development;
    }
    reader.endArray();
    throw new IOException("Something went wrong");
  }

  public JsonObject encrypt() {
    JsonObject jsonObject = (JsonObject) new Gson().toJsonTree(this.settings);
    traverse(jsonObject, true);
    System.out.print(jsonObject);
    return jsonObject;
  }

  public JsonObject decrypt() {
    JsonObject jsonObject = (JsonObject) new Gson().toJsonTree(this.settings);
    traverse(jsonObject, false);
    System.out.print(jsonObject);
    return jsonObject;
  }

  public String encryptField(JsonElement field) throws Exception {
    byte[] plain = field.getAsString().getBytes(this.charset);
    byte[] ciphertext = this.cipher.doFinal(plain);
    return new String(Base64.getEncoder().encode(ciphertext));
  }

  public String encryptField(String field) throws Exception {
    byte[] plain = field.getBytes(this.charset);
    byte[] ciphertext = this.cipher.doFinal(plain);
    return new String(Base64.getEncoder().encode(ciphertext));
  }

  public String decryptField(JsonElement field) {
    try {
      byte[] plain = Base64.getDecoder().decode(field.getAsString());
      this.cipher.init(
          Cipher.DECRYPT_MODE,
          new SecretKeySpec(this.encryptedKey.getEncoded(), this.symmetricAlg),
          new IvParameterSpec(this.iv)
        );
      return new String(this.cipher.doFinal(plain), this.charset);
    } catch (Throwable e) {
      System.out.println(e);
      return "ERROR_DURING_FIELD_DECRYPTION";
    }
  }

  public String decryptField(String field) {
    try {
      byte[] plain = Base64.getDecoder().decode(field);
      this.cipher.init(
          Cipher.DECRYPT_MODE,
          new SecretKeySpec(this.encryptedKey.getEncoded(), this.symmetricAlg),
          new IvParameterSpec(this.iv)
        );
      return new String(this.cipher.doFinal(plain), this.charset);
    } catch (Throwable e) {
      System.out.println(e);
      return "ERROR_DURING_FIELD_DECRYPTION";
    }
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
        Matcher matcher = this.doubleDollarSignPattern.matcher(entry.getKey());
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
            e.printStackTrace();
          }
          entry.setValue(replaceWith.get(entry.getKey()));
        }
      } else if (entry.getValue() instanceof JsonObject) {
        traverse(entry.getValue(), encrypt);
      } else {
        return;
      }
    }
  }
}
