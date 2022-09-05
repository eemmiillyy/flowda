import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import flow.core.Bootstrap;
import flow.core.Job.JobSource;
import flow.core.Kafka.KafkaClient;
import flow.core.Settings.Settings;
import flow.core.Utils.JWT;
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
public class CreateQueryEndpointTest {

  Bootstrap mockedAppServer;
  Future<HttpServer> mockServerFlink;
  Future<HttpServer> futureApp;
  HttpServer mockServerFlinkBeforeInit;
  Settings settings = new Settings();
  Vertx vertx = Vertx.vertx();
  VertxTestContext testContext;

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
    Mockito.doNothing().when(kafkaShellClientSpy).modifyACL(Mockito.any());
    return kafkaShellClientSpy;
  }

  public void launchAppWithTestSettings(
    KafkaClient kafkaShellStub,
    JobSource flinkStub
  )
    throws IOException, InterruptedException {
    Bootstrap bootstrap = new Bootstrap();
    this.mockedAppServer = Mockito.spy(bootstrap);
    this.mockedAppServer.kafkaClient = kafkaShellStub;
    this.mockedAppServer.jobSource = flinkStub;
    this.futureApp = this.mockedAppServer.start();
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

  public void runTest(String input, String output, String jwt)
    throws InterruptedException {
    this.futureApp.onSuccess(
        app -> {
          System.out.println("APP UP" + app);
          this.mockServerFlink.onSuccess(
              server -> {
                System.out.println(
                  "Mock server started on port" + server.actualPort()
                );
                WebClient client = WebClient.create(vertx);
                String testJWT = "";
                try {
                  testJWT = jwt == "" ? new JWT().create("tester") : jwt;
                } catch (
                  InvalidKeyException
                  | NoSuchAlgorithmException
                  | JSONException e
                ) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
                }
                client
                  .post(8888, "localhost", "/createQuery")
                  .bearerTokenAuthentication(testJWT)
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
      e.printStackTrace();
    }
    assertTrue(testContext.completed() == true);
  }

  @Test
  public void testSucceedsWithValidArugmentsQuery()
    throws InterruptedException {
    runTest(
      "{\"connectionString\": \"mysql://debezium:dbz@mysql:3306/inventory\",\"environmentId\": \"tester\",\"sourceSql\": \"CREATE TABLE products_on_hand (quantity INT, product_id INT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\",\"sourceSqlTableTwo\": \"CREATE TABLE orders (order_number BIGINT, purchaser BIGINT, quantity BIGINT, product_id BIGINT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL,  WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\", \"querySql\": \"SELECT SUM(quantity) as summed FROM products_on_hand\",\"sinkSql\": \"CREATE TABLE custom_output_table_name (summed INT)\" }",
      "\"jobId\":\"mockJobId\"",
      ""
    );
  }

  @Test
  public void testThrowsWithMissingConnectionStringQuery()
    throws InterruptedException {
    runTest(
      "{\"environmentId\": \"tester\",\"sourceSql\": \"CREATE TABLE products_on_hand (quantity INT, product_id INT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\",\"sourceSqlTableTwo\": \"CREATE TABLE orders (order_number BIGINT, purchaser BIGINT, quantity BIGINT, product_id BIGINT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL,  WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\", \"querySql\": \"SELECT SUM(quantity) as summed FROM products_on_hand\",\"sinkSql\": \"CREATE TABLE custom_output_table_name (summed INT)\" }",
      "{\"message\":\"Client error. Missing user input:connectionString are missing.\",\"code\":4001}",
      ""
    );
  }

  @Test
  public void testThrowsWithMissingSourceSqlQuery()
    throws InterruptedException {
    runTest(
      "{\"connectionString\": \"mysql://debezium:dbz@mysql:3306/inventory\",\"environmentId\": \"tester\", \"sourceSqlTableTwo\": \"CREATE TABLE orders (order_number BIGINT, purchaser BIGINT, quantity BIGINT, product_id BIGINT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL,  WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\", \"querySql\": \"SELECT SUM(quantity) as summed FROM products_on_hand\",\"sinkSql\": \"CREATE TABLE custom_output_table_name (summed INT)\" }",
      "{\"message\":\"Client error. Missing user input:sourceSql are missing.\",\"code\":4001}",
      ""
    );
  }

  @Test
  public void testThrowsWithMissingSourceSqlTableTwoQuery()
    throws InterruptedException {
    runTest(
      "{\"connectionString\": \"mysql://debezium:dbz@mysql:3306/inventory\",\"environmentId\": \"tester\",\"sourceSql\": \"CREATE TABLE products_on_hand (quantity INT, product_id INT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\", \"querySql\": \"SELECT SUM(quantity) as summed FROM products_on_hand\",\"sinkSql\": \"CREATE TABLE custom_output_table_name (summed INT)\" }",
      "{\"message\":\"Client error. Missing user input:sourceSqlTableTwo are missing.\",\"code\":4001}",
      ""
    );
  }

  @Test
  public void testThrowsWithMissingQuerySql() throws InterruptedException {
    runTest(
      "{\"connectionString\": \"mysql://debezium:dbz@mysql:3306/inventory\",\"environmentId\": \"tester\",\"sourceSql\": \"CREATE TABLE products_on_hand (quantity INT, product_id INT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\",\"sourceSqlTableTwo\": \"CREATE TABLE orders (order_number BIGINT, purchaser BIGINT, quantity BIGINT, product_id BIGINT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL,  WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\", \"sinkSql\": \"CREATE TABLE custom_output_table_name (summed INT)\" }",
      "{\"message\":\"Client error. Missing user input:querySql are missing.\",\"code\":4001}",
      ""
    );
  }

  @Test
  public void testThrowsWithMissingSinkSqlQuery() throws InterruptedException {
    runTest(
      "{\"connectionString\": \"mysql://debezium:dbz@mysql:3306/inventory\",\"environmentId\": \"tester\",\"sourceSql\": \"CREATE TABLE products_on_hand (quantity INT, product_id INT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\",\"sourceSqlTableTwo\": \"CREATE TABLE orders (order_number BIGINT, purchaser BIGINT, quantity BIGINT, product_id BIGINT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL,  WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\", \"querySql\": \"SELECT SUM(quantity) as summed FROM products_on_hand\" }",
      "{\"message\":\"Client error. Missing user input:sinkSql are missing.\",\"code\":4001}",
      ""
    );
  }

  @Test
  public void testThrowsWithMissingArguments() throws InterruptedException {
    runTest(
      "{}",
      "{\"message\":\"Client error. Missing user input:connectionString,sourceSql,sourceSqlTableTwo,querySql,sinkSql are missing.\",\"code\":4001}",
      ""
    );
  }

  @Test
  public void testThrowsWithInvalidConnectionStringArguments()
    throws InterruptedException {
    runTest(
      "{\"connectionString\": \"://debezium:dbz@mysql:3306/inventory\",\"environmentId\": \"tester\",\"sourceSql\": \"CREATE TABLE products_on_hand (quantity INT, product_id INT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\",\"sourceSqlTableTwo\": \"CREATE TABLE orders (order_number BIGINT, purchaser BIGINT, quantity BIGINT, product_id BIGINT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL,  WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\", \"querySql\": \"SELECT SUM(quantity) as summed FROM products_on_hand\",\"sinkSql\": \"CREATE TABLE custom_output_table_name (summed INT)\" }",
      "{\"message\":\"Client error. Invalid user input:VALIDATION EXCEPTION: Connection string does not use a valid mysql protocol. A valid protocol looks like: mysql://...\",\"code\":4002}",
      ""
    );
  }

  @Test
  public void testThrowsWithInvalidSqlIfContainsInvalidCharacters()
    throws InterruptedException {
    runTest(
      "{\"connectionString\": \"mysql://debezium:dbz@mysql:3306/inventory\",\"environmentId\": \"tester\",\"sourceSql\": \"CREATE TABLE products_on_hand (quantity INT, product_id INT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND);\",\"sourceSqlTableTwo\": \"CREATE TABLE orders (order_number BIGINT, purchaser BIGINT, quantity BIGINT, product_id BIGINT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL,  WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\", \"querySql\": \"SELECT SUM(quantity) as summed FROM products_on_hand\",\"sinkSql\": \"CREATE TABLE custom_output_table_name (summed INT)\" }",
      "{\"message\":\"Client error. Invalid user input:VALIDATION EXCEPTION: sourceSql must be alpha numeric characters only, as well as (),_-.'\",\"code\":4002}",
      ""
    );
  }

  @Test
  public void testThrowsServerErrorUnableToCreateFlinkJob()
    throws InterruptedException {
    this.mockServerFlinkBeforeInit.close();
    runTest(
      "{\"connectionString\": \"mysql://debezium:dbz@mysql:3306/inventory\",\"environmentId\": \"tester\",\"sourceSql\": \"CREATE TABLE products_on_hand (quantity INT, product_id INT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\",\"sourceSqlTableTwo\": \"CREATE TABLE orders (order_number BIGINT, purchaser BIGINT, quantity BIGINT, product_id BIGINT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL,  WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\", \"querySql\": \"SELECT SUM(quantity) as summed FROM products_on_hand\",\"sinkSql\": \"CREATE TABLE custom_output_table_name (summed INT)\" }",
      "{\"message\":\"Internal server error. Unable to create job with Flink:Connection refused: localhost/127.0.0.1:9001\",\"code\":4007}",
      ""
    );
  }

  @Test
  public void testThrowsClientErrorAuthorization() throws InterruptedException {
    runTest(
      "{\"connectionString\": \"mysql://debezium:dbz@mysql:3306/inventory\",\"environmentId\": \"tester\",\"sourceSql\": \"CREATE TABLE products_on_hand (quantity INT, product_id INT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\",\"sourceSqlTableTwo\": \"CREATE TABLE orders (order_number BIGINT, purchaser BIGINT, quantity BIGINT, product_id BIGINT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL,  WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)\", \"querySql\": \"SELECT SUM(quantity) as summed FROM products_on_hand\",\"sinkSql\": \"CREATE TABLE custom_output_table_name (summed INT)\" }",
      "{\"message\":\"Client error. Invalid authorization jwt header:JWT is expired\",\"code\":4005}",
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbnZpcm9ubWVudElkIjoic2VhQWFhYUFhYWFhYWEiLCJleHAiOiIyMDIyLTA4LTA5In0.k-ylXHwdIfBsRK2glX70d_onBP9xNSp3mAJVjl4a7Gc"
    );
  }
  // TODO test for ACL generation
  // TODO test bad connection

}
