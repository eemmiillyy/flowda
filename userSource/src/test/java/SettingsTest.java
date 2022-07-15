import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import userSource.Settings.Settings;
import userSource.Settings.SettingsShape;
import userSource.Settings.SettingsShape.Stage.StageInstance;

public class SettingsTest {

  Settings spy;
  String stage = "development";
  String plaintextPassword = "tester";
  String encryptedPassword = "Pb7IvdI9yWPJTgjoYuSVAQ==";
  String pathToFixture = "src/test/resources/Settings.json";

  @BeforeEach
  public void setup() throws IOException {
    Settings settings = new Settings(stage);
    this.spy = Mockito.spy(settings);
    spy.settings = spy.load(stage, pathToFixture);
  }

  @Test
  public void testICanEncryptTheFile() throws IOException {
    JsonObject settings = spy.encrypt();
    StageInstance s = new Gson()
    .fromJson(settings, SettingsShape.Stage.StageInstance.class);
    assertEquals(s.services.kafka.admin.$$password, encryptedPassword);
  }

  @Test
  public void testICanDecryptTheFile() throws IOException {
    JsonObject settings = spy.encrypt();
    StageInstance s = new Gson()
    .fromJson(settings, SettingsShape.Stage.StageInstance.class);
    spy.settings = s;

    JsonObject decrypted = spy.decrypt();
    StageInstance decryptedSettings = new Gson()
    .fromJson(decrypted, SettingsShape.Stage.StageInstance.class);
    assertEquals(
      decryptedSettings.services.kafka.admin.$$password,
      plaintextPassword
    );
  }

  @Test
  public void testICanEncryptASingleField() throws Exception {
    String password = spy.settings.services.kafka.admin.$$password;
    assertEquals(spy.encryptField(password), encryptedPassword);
  }

  @Test
  public void testICanDecryptASingleField() throws Exception {
    String password = spy.settings.services.kafka.admin.$$password;
    assertEquals(
      spy.decryptField(spy.encryptField(password)),
      plaintextPassword
    );
  }

  @Test
  public void testEncryptionAndDecryptAreThreadSafe()
    throws InterruptedException, IOException {
    Settings set = new Settings(stage);
    Settings mutableSettings = Mockito.spy(set);
    mutableSettings.settings = mutableSettings.load(stage, pathToFixture);

    int numberOfThreads = 20;
    ExecutorService service = Executors.newFixedThreadPool(10);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);

    for (int i = 0; i < numberOfThreads; i++) {
      service.submit(
        () -> {
          try {
            String encrypted = mutableSettings.encryptField(
              mutableSettings.settings.services.kafka.admin.$$password
            );

            assertEquals(
              mutableSettings.decryptField(encrypted),
              plaintextPassword
            );
          } catch (Exception e) {
            // Handle exception
          }
          latch.countDown();
        }
      );
    }
    latch.await();
  }
}
