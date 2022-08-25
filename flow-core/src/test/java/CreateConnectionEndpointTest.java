import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import flow.core.Bootstrap;
import flow.core.Settings.Settings;
import flow.core.Utils.ConnectionChecker;
import flow.core.Utils.ConnectionChecker.AccessDeniedError;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class CreateConnectionEndpointTest {

  Bootstrap mockedAppServer;
  ConnectionChecker connectionCheckerStub;
  Future<HttpServer> futureApp;
  Future<HttpServer> mockServerFlink;
  Future<HttpServer> mockServerDebezium;
  HttpServer mockServerDebeziumBeforeInit;
  HttpServer mockServerFlinkBeforeInit;
  Settings settings = new Settings();
  Vertx vertx = Vertx.vertx();
  VertxTestContext testContext;

  @BeforeEach
  public void setup(TestInfo testInfo)
    throws IOException, InterruptedException, AccessDeniedError, ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException {
    this.testContext = new VertxTestContext();
    stubConnectionChecker();
    startDebeziumServerMock();
    launchAppWithTestSettings();
  }

  public void stubConnectionChecker()
    throws AccessDeniedError, ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException {
    ConnectionChecker checkConn = new ConnectionChecker();
    this.connectionCheckerStub = Mockito.spy(checkConn);
    Mockito
      .doReturn(true)
      .when(connectionCheckerStub)
      .canConnect(Mockito.any());
  }

  public void launchAppWithTestSettings()
    throws IOException, InterruptedException {
    Bootstrap bootstrap = new Bootstrap();
    this.mockedAppServer = Mockito.spy(bootstrap);
    this.mockedAppServer.connectionChecker = this.connectionCheckerStub;
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
      "{\"message\":\"Client error. Missing user input:connectionString are missing.\",\"code\":4001}"
    );
  }

  @Test
  public void testThrowsWithMissingEnvironmentId() throws Throwable {
    runTest(
      "{\"connectionString\": \"mysql://u:p@127.0.0.1:3306/inventory\"}",
      "{\"message\":\"Client error. Missing user input:environmentId are missing.\",\"code\":4001}"
    );
  }

  @Test
  public void testFailsWithNoArguments() throws Throwable {
    runTest(
      "{}",
      "{\"message\":\"Client error. Missing user input:connectionString,environmentId are missing.\",\"code\":4001}"
    );
  }
}
