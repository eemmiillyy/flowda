import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import userSource.Bootstrap;
import userSource.Settings.Settings;

@ExtendWith(VertxExtension.class)
public class CreateConnectionEndpointTest {

  static String stage = "test";
  Vertx vertx = Vertx.vertx();
  VertxTestContext testContext;
  HttpServer mockServerDebeziumBeforeInit;
  Future<HttpServer> mockServerDebezium;
  HttpServer mockServerFlinkBeforeInit;
  Future<HttpServer> mockServerFlink;
  String matcher = "tester";
  Settings settings = new Settings();
  Bootstrap mockedAppServer;
  Future<HttpServer> futureApp;

  @BeforeEach
  public void setup(TestInfo testInfo)
    throws IOException, InterruptedException {
    this.testContext = new VertxTestContext();
    startDebeziumServerMock();
    launchAppWithTestSettings();
  }

  public void launchAppWithTestSettings()
    throws IOException, InterruptedException {
    Bootstrap bootstrap = new Bootstrap();
    this.mockedAppServer = Mockito.spy(bootstrap);
    this.futureApp = this.mockedAppServer.start();
  }

  public void startDebeziumServerMock() {
    this.mockServerDebeziumBeforeInit = vertx.createHttpServer();

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router
      .route("/connectors")
      .handler(
        context -> {
          context.json(new JsonObject().put("name", "mockConnector"));
        }
      );
    this.mockServerDebezium =
      mockServerDebeziumBeforeInit
        .requestHandler(router)
        .listen(9000)
        .onFailure(message -> System.out.println(message));
  }

  @AfterEach
  public void teardown() {
    this.mockServerDebeziumBeforeInit.close();
    this.mockServerDebeziumBeforeInit = null;
    this.mockServerDebezium = null;
    this.mockedAppServer.close();
  }

  public void runTest(String input, String output) throws InterruptedException {
    this.futureApp.onSuccess(
        app -> {
          System.out.println("APP UP" + app);
          this.mockServerDebezium.onSuccess(
              server -> {
                System.out.println(
                  "Mock server started on port" + server.actualPort()
                );

                WebClient client = WebClient.create(vertx);
                client
                  .post(8888, "localhost", "/createConnection")
                  .sendJsonObject(new JsonObject(input))
                  .onComplete(
                    testContext.succeeding(
                      buffer ->
                        testContext.verify(
                          () -> {
                            System.out.println(buffer.bodyAsString());
                            assertTrue(buffer.bodyAsString().contains(output));
                            testContext.completeNow();
                          }
                        )
                    )
                  );
              }
            );
        }
      );
    try {
      testContext.awaitCompletion(20, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    assertTrue(testContext.completed() == true);
  }

  @Test
  public void testSucceedsWithValidArugments() throws Throwable {
    runTest(
      "{\"connectionString\": \"mysql://u:p@127.0.0.1:3306/inventory\", \"environmentId\": \"u\"}",
      "{\"data\":\"mockConnector\"}"
    );
  }

  @Test
  public void testThrowsWithMissingConnectionString() throws Throwable {
    runTest(
      "{ \"environmentId\": \"u\"}",
      "{\"message\":\"connectionString are missing.\",\"code\":4001}"
    );
  }

  @Test
  public void testThrowsWithMissingEnvironmentId() throws Throwable {
    runTest(
      "{\"connectionString\": \"mysql://u:p@127.0.0.1:3306/inventory\"}",
      "{\"message\":\"environmentId are missing.\",\"code\":4001}"
    );
  }

  @Test
  public void testFailsWithNoArguments() throws Throwable {
    runTest(
      "{}",
      "{\"message\":\"connectionString,environmentId are missing.\",\"code\":4001}"
    );
  }
}
