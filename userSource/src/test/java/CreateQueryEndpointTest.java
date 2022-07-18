import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
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
import userSource.Flink.FlinkArtifactGenerator;
import userSource.Kafka.KafkaClient;
import userSource.Kafka.KafkaShellClient;
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
  public void setup(TestInfo testInfo)
    throws IOException, InterruptedException {
    this.testContext = new VertxTestContext();
    startFlinkServerMock();
    FlinkArtifactGenerator flinkStub = createFlinkStub();
    KafkaShellClient kafkaShellStub = createKafkaShellStub();
    launchAppWithTestSettings(kafkaShellStub, flinkStub);
  }

  public HashMap<String, List<PartitionInfo>> dummySubscribeOutput() {
    HashMap<String, List<PartitionInfo>> topicMap = new HashMap<String, List<PartitionInfo>>();
    List<PartitionInfo> partitionInfoList = new ArrayList<PartitionInfo>();
    Node node = new Node(1, "", 1);
    PartitionInfo partitionInfo = new PartitionInfo(
      matcher,
      0,
      node,
      null,
      null
    );
    partitionInfoList.add(partitionInfo);
    topicMap.put(matcher, partitionInfoList);
    return topicMap;
  }

  public ConsumerRecords<String, String> dummyPollOutput() {
    Map<TopicPartition, List<ConsumerRecord<String, String>>> map = new HashMap<TopicPartition, List<ConsumerRecord<String, String>>>();
    List<ConsumerRecord<String, String>> consumerRecordList = new ArrayList<ConsumerRecord<String, String>>();
    consumerRecordList.add(
      new ConsumerRecord<String, String>(
        matcher,
        0,
        0,
        matcher,
        String.format(
          "{\"payload\": { \"source\": {\"table\": \"%s\", \"name\": \"%<s\"}, \"tableChanges\": [{\"type\": \"CREATE\", \"id\": \"%<s\", \"table\": { \"columns\": [{\"name\": \"%<s\", \"typeName\": \"INT\", \"sql\": \"CREATE TABLE `%<s`\"}]}}], \"databaseName\": \"%<s\"}}",
          matcher
        )
      )
    );
    TopicPartition part = new TopicPartition(matcher, 0);
    map.put(part, consumerRecordList);
    ConsumerRecords<String, String> records = new ConsumerRecords<String, String>(
      map
    );
    System.out.println(records.toString());
    return records;
  }

  public FlinkArtifactGenerator createFlinkStub() {
    // Replace kafka consumer methods with dummy output or do nothing
    FlinkArtifactGenerator flinkStub = new FlinkArtifactGenerator(
      this.settings
    );
    FlinkArtifactGenerator flinkStubSpy = Mockito.spy(flinkStub);
    KafkaConsumer<String, String> client = new KafkaClient(
      "",
      "",
      this.settings
    )
    .create("");
    KafkaConsumer<String, String> clientSpy = Mockito.spy(client);
    Mockito
      .doReturn(dummySubscribeOutput())
      .when(clientSpy)
      .listTopics(Mockito.any());
    Mockito.doReturn(dummyPollOutput()).when(clientSpy).poll(1000);
    Mockito.doNothing().when(clientSpy).subscribe(Mockito.anyCollection());
    Mockito.doNothing().when(clientSpy).close();
    Mockito.doReturn(clientSpy).when(flinkStubSpy).createKafkaConsumer();
    return flinkStubSpy;
  }

  public KafkaShellClient createKafkaShellStub()
    throws IOException, InterruptedException {
    KafkaShellClient kafkaShellClient = new KafkaShellClient(this.settings);
    KafkaShellClient kafkaShellClientSpy = Mockito.spy(kafkaShellClient);
    // Do not actually run ACLs
    Mockito.doNothing().when(kafkaShellClientSpy).run(Mockito.any());
    return kafkaShellClientSpy;
  }

  public void launchAppWithTestSettings(
    KafkaShellClient kafkaShellStub,
    FlinkArtifactGenerator flinkStub
  )
    throws IOException, InterruptedException {
    Bootstrap bootstrap = new Bootstrap();
    this.mockedAppServer = Mockito.spy(bootstrap);
    mockedAppServer.kafkaShellClient = kafkaShellStub;
    mockedAppServer.flinkArtifactGenerator = flinkStub;
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
      "{\"connectionString\": \"mysql://debezium:dbz@mysql:3306/inventory\",\"environmentId\": \"tester\",\"databaseName\": \"tester\",\"tableName\": \"tester\",\"fieldName\": \"tester\" }",
      "\"jobId\":\"mockJobId\""
    );
  }

  @Test
  public void testThrowsWithMissingEnvironmentIdQuery()
    throws InterruptedException {
    runTestQuery(
      "{\"connectionString\": \"mysql://debezium:dbz@mysql:3306/inventory\",\"databaseName\": \"inventory\",\"tableName\": \"products_on_hand\",\"fieldName\": \"quantity\" }",
      "{\"message\":\"environmentId are missing.\",\"code\":4001}"
    );
  }

  @Test
  public void testThrowsWithMissingConnectionStringQuery()
    throws InterruptedException {
    runTestQuery(
      "{\"environmentId\": \"test\", \"databaseName\": \"inventory\",\"tableName\": \"products_on_hand\",\"fieldName\": \"quantity\" }",
      "{\"message\":\"connectionString are missing.\",\"code\":4001}"
    );
  }

  @Test
  public void testThrowsWithMissingTableNameQuery()
    throws InterruptedException {
    runTestQuery(
      "{\"connectionString\": \"mysql://debezium:dbz@mysql:3306/inventory\", \"environmentId\": \"test\", \"databaseName\": \"inventory\",\"fieldName\": \"quantity\" }",
      "{\"message\":\"tableName are missing.\",\"code\":4001}"
    );
  }

  @Test
  public void testThrowsWithMissingFieldNameQuery()
    throws InterruptedException {
    runTestQuery(
      "{\"connectionString\": \"mysql://debezium:dbz@mysql:3306/inventory\", \"environmentId\": \"test\", \"databaseName\": \"inventory\",\"tableName\": \"products_on_hand\" }",
      "{\"message\":\"fieldName are missing.\",\"code\":4001}"
    );
  }

  @Test
  public void testThrowsWithMissingDatabaseName() throws InterruptedException {
    runTestQuery(
      "{\"connectionString\": \"mysql://debezium:dbz@mysql:3306/inventory\", \"environmentId\": \"test\", \"tableName\": \"products_on_hand\",\"fieldName\": \"quantity\" }",
      "{\"message\":\"databaseName are missing.\",\"code\":4001}"
    );
  }

  @Test
  public void testThrowsWithMissingArguments() throws InterruptedException {
    runTestQuery(
      "{}",
      "{\"message\":\"connectionString,environmentId,databaseName,tableName,fieldName are missing.\",\"code\":4001}"
    );
  }
}
