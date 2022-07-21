import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import userSource.Bootstrap;
import userSource.Connector.Kafka.KafkaClient;
import userSource.Job.JobSource;
import userSource.Settings.Settings;

@ExtendWith(VertxExtension.class)
public class CreateQueryEndpointTest {

  static String stage = "test";
  Vertx vertx = Vertx.vertx();
  VertxTestContext testContext;
  HttpServer mockServerFlinkBeforeInit;
  Future<HttpServer> mockServerFlink;
  String matcher = "tester";
  Settings settings = new Settings();
  Bootstrap mockedAppServer;
  Future<HttpServer> futureApp;

  @BeforeEach
  public void setup(TestInfo testInfo) throws Exception {
    this.testContext = new VertxTestContext();
    startFlinkServerMock();
    JobSource flinkStub = createFlinkStub();
    KafkaClient kafkaShellStub = createKafkaShellStub();
    launchAppWithTestSettings(kafkaShellStub, flinkStub);
  }

  public JobSource createFlinkStub() throws Exception {
    // Replace kafka consumer methods with dummy output or do nothing
    JobSource flinkStub = new JobSource(this.settings);
    JobSource flinkStubSpy = Mockito.spy(flinkStub);

    return flinkStubSpy;
  }

  public KafkaClient createKafkaShellStub()
    throws IOException, InterruptedException {
    KafkaClient kafkaShellClient = new KafkaClient(this.settings);
    KafkaClient kafkaShellClientSpy = Mockito.spy(kafkaShellClient);
    // Do not actually run ACLs
    Mockito.doNothing().when(kafkaShellClientSpy).run(Mockito.any());
    return kafkaShellClientSpy;
  }

  public void launchAppWithTestSettings(
    KafkaClient kafkaShellStub,
    JobSource flinkStub
  )
    throws IOException, InterruptedException {
    Bootstrap bootstrap = new Bootstrap();
    this.mockedAppServer = Mockito.spy(bootstrap);
    mockedAppServer.kafkaClient = kafkaShellStub;
    mockedAppServer.jobSource = flinkStub;
  }

  public void startFlinkServerMock() {
    this.mockServerFlinkBeforeInit = vertx.createHttpServer();

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router
      .route("/run")
      .handler(
        context -> {
          context.json(new JsonObject().put("jobid", "mockJobId"));
        }
      );
    this.mockServerFlink =
      mockServerFlinkBeforeInit
        .requestHandler(router)
        .listen(9001)
        .onFailure(message -> System.out.println(message));
  }

  @AfterEach
  public void teardown() {
    this.mockServerFlinkBeforeInit.close();
    this.mockServerFlinkBeforeInit = null;
    this.mockServerFlink = null;
    this.mockedAppServer.close();
  }

  // // TODO MOVE
  public void runTestQuery(String input, String output)
    throws InterruptedException {
    System.out.println("RUNNING TEST QUERY");

    this.mockedAppServer.start()
      .onSuccess(
        app -> {
          System.out.println("APP UP" + app);
          this.mockServerFlink.onSuccess(
              server -> {
                System.out.println(
                  "Mock server started on port" + server.actualPort()
                );
                WebClient client = WebClient.create(vertx);
                client
                  .post(8888, "localhost", "/createQuery")
                  .sendJsonObject(new JsonObject(input))
                  .onComplete(
                    testContext.succeeding(
                      buffer -> {
                        System.out.println(buffer.bodyAsString());
                        testContext.verify(
                          () -> {
                            assertTrue(buffer.bodyAsString().contains(output));
                            testContext.completeNow();
                          }
                        );
                      }
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
  public void testSucceedsWithValidArugmentsQuery()
    throws InterruptedException {
    runTestQuery(
      "{\"connectionString\": \"mysql://debezium:dbz@mysql:3306/inventory\",\"environmentId\": \"tester\",\"sourceSql\": \"CREATE TABLE products_on_hand (quantity INT, product_id INT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\",\"sourceSqlTableTwo\": \"CREATE TABLE orders (order_number BIGINT, purchaser BIGINT, quantity BIGINT, product_id BIGINT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL,  WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\", \"querySql\": \"SELECT SUM(quantity) as summed FROM products_on_hand\",\"sinkSql\": \"CREATE TABLE custom_output_table_name (summed INT)\" }",
      "\"jobId\":\"mockJobId\""
    );
  }

  @Test
  public void testThrowsWithMissingEnvironmentIdQuery()
    throws InterruptedException {
    runTestQuery(
      "{\"connectionString\": \"mysql://debezium:dbz@mysql:3306/inventory\",\"sourceSql\": \"CREATE TABLE products_on_hand (quantity INT, product_id INT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\",\"sourceSqlTableTwo\": \"CREATE TABLE orders (order_number BIGINT, purchaser BIGINT, quantity BIGINT, product_id BIGINT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL,  WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\", \"querySql\": \"SELECT SUM(quantity) as summed FROM products_on_hand\",\"sinkSql\": \"CREATE TABLE custom_output_table_name (summed INT)\" }",
      "{\"message\":\"environmentId are missing.\",\"code\":4001}"
    );
  }

  @Test
  public void testThrowsWithMissingConnectionStringQuery()
    throws InterruptedException {
    runTestQuery(
      "{\"environmentId\": \"tester\",\"sourceSql\": \"CREATE TABLE products_on_hand (quantity INT, product_id INT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\",\"sourceSqlTableTwo\": \"CREATE TABLE orders (order_number BIGINT, purchaser BIGINT, quantity BIGINT, product_id BIGINT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL,  WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\", \"querySql\": \"SELECT SUM(quantity) as summed FROM products_on_hand\",\"sinkSql\": \"CREATE TABLE custom_output_table_name (summed INT)\" }",
      "{\"message\":\"connectionString are missing.\",\"code\":4001}"
    );
  }

  @Test
  public void testThrowsWithMissingSourceSqlQuery()
    throws InterruptedException {
    runTestQuery(
      "{\"connectionString\": \"mysql://debezium:dbz@mysql:3306/inventory\",\"environmentId\": \"tester\", \"sourceSqlTableTwo\": \"CREATE TABLE orders (order_number BIGINT, purchaser BIGINT, quantity BIGINT, product_id BIGINT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL,  WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\", \"querySql\": \"SELECT SUM(quantity) as summed FROM products_on_hand\",\"sinkSql\": \"CREATE TABLE custom_output_table_name (summed INT)\" }",
      "{\"message\":\"sourceSql are missing.\",\"code\":4001}"
    );
  }

  @Test
  public void testThrowsWithMissingSourceSqlTableTwoQuery()
    throws InterruptedException {
    runTestQuery(
      "{\"connectionString\": \"mysql://debezium:dbz@mysql:3306/inventory\",\"environmentId\": \"tester\",\"sourceSql\": \"CREATE TABLE products_on_hand (quantity INT, product_id INT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\", \"querySql\": \"SELECT SUM(quantity) as summed FROM products_on_hand\",\"sinkSql\": \"CREATE TABLE custom_output_table_name (summed INT)\" }",
      "{\"message\":\"sourceSqlTableTwo are missing.\",\"code\":4001}"
    );
  }

  @Test
  public void testThrowsWithMissingQuerySql() throws InterruptedException {
    runTestQuery(
      "{\"connectionString\": \"mysql://debezium:dbz@mysql:3306/inventory\",\"environmentId\": \"tester\",\"sourceSql\": \"CREATE TABLE products_on_hand (quantity INT, product_id INT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\",\"sourceSqlTableTwo\": \"CREATE TABLE orders (order_number BIGINT, purchaser BIGINT, quantity BIGINT, product_id BIGINT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL,  WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\", \"sinkSql\": \"CREATE TABLE custom_output_table_name (summed INT)\" }",
      "{\"message\":\"querySql are missing.\",\"code\":4001}"
    );
  }

  @Test
  public void testThrowsWithMissingSinkSqlQuery() throws InterruptedException {
    runTestQuery(
      "{\"connectionString\": \"mysql://debezium:dbz@mysql:3306/inventory\",\"environmentId\": \"tester\",\"sourceSql\": \"CREATE TABLE products_on_hand (quantity INT, product_id INT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\",\"sourceSqlTableTwo\": \"CREATE TABLE orders (order_number BIGINT, purchaser BIGINT, quantity BIGINT, product_id BIGINT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL,  WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\", \"querySql\": \"SELECT SUM(quantity) as summed FROM products_on_hand\" }",
      "{\"message\":\"sinkSql are missing.\",\"code\":4001}"
    );
  }

  @Test
  public void testThrowsWithMissingArguments() throws InterruptedException {
    runTestQuery(
      "{}",
      "{\"message\":\"connectionString,environmentId,sourceSql,sourceSqlTableTwo,querySql,sinkSql are missing.\",\"code\":4001}"
    );
  }
}
