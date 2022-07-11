import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import userSource.Bootstrap;
import userSource.Settings.Settings;

@ExtendWith(VertxExtension.class)
public class DebeziumTest {

  static String stage = "development";
  static String pathToFixture =
    "/Users/emilymorgan/Desktop/pdpDataProjections/userSource/src/test/resources/Settings.json";
  Bootstrap bootstrapSpy;
  Vertx vertx = Vertx.vertx();

  // Mock debezium client to stub response
  // Check I can successfully submit it
  // Check if all arguments are present
  // Check if all arguments are valid
  // Check there are no thread errors
  // @BeforeAll
  @Test
  public void testCanFormatConnectionString() throws Throwable {
    System.out.println("Settings up....");
    Settings settings = new Settings(stage);
    Settings settingsSpy = Mockito.spy(settings);
    settingsSpy.settings = settingsSpy.load(stage, pathToFixture);

    Bootstrap bootstrap = new Bootstrap(stage);
    Bootstrap spy = Mockito.spy(bootstrap);
    this.bootstrapSpy = spy;
    spy.settings = settingsSpy;
    spy.start();

    VertxTestContext testContext = new VertxTestContext();

    io.vertx.core.http.HttpServer mockServer = vertx.createHttpServer();

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    router
      .route("/connectors")
      .handler(
        context -> {
          System.out.println("MOCK CONNECTORS HIT");
          System.out.println(context.body());
          context.json(new JsonObject().put("name", "mockConnector"));
        }
      );

    mockServer
      .requestHandler(router)
      .listen(9000)
      .onFailure(message -> System.out.println(message))
      .onSuccess(
        server -> {
          System.out.println(
            "Mock server started on port" + server.actualPort()
          );

          WebClient client = WebClient.create(vertx);

          client
            .post(8888, "localhost", "/createConnection")
            .sendJsonObject(
              new JsonObject(
                "{\"connectionString\": \"mysql://u:p@127.0.0.1:3306/inventory\", \"environmentId\": \"u\"}"
              )
            )
            .onComplete(
              testContext.succeeding(
                buffer ->
                  testContext.verify(
                    () -> {
                      assertTrue(
                        buffer
                          .bodyAsString()
                          .contentEquals("{\"data\":\"mockConnector\"}")
                      );
                    }
                  )
              )
            );
        }
      );

    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }
}
