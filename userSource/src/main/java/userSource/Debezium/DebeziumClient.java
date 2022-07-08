package userSource.Debezium;

import java.net.URI;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import userSource.Settings.Settings;
import userSource.Settings.SettingsShape.Stage.StageInstance;

public class DebeziumClient {

  // {
  //     "name": "hdfs-sink-connector",
  //     "config": {
  //         "connector.class": "io.confluent.connect.hdfs.HdfsSinkConnector",
  //         "tasks.max": "10",
  //         "topics": "test-topic",
  //         "hdfs.url": "hdfs://fakehost:9000",
  //         "hadoop.conf.dir": "/opt/hadoop/conf",
  //         "hadoop.home": "/opt/hadoop",
  //         "flush.size": "100",
  //         "rotate.interval.ms": "1000"
  //     }
  // }
  // RESPONSE - 201
  // {
  //     "name": "hdfs-sink-connector",
  //     "config": {
  //         "connector.class": "io.confluent.connect.hdfs.HdfsSinkConnector",
  //         "tasks.max": "10",
  //         "topics": "test-topic",
  //         "hdfs.url": "hdfs://fakehost:9000",
  //         "hadoop.conf.dir": "/opt/hadoop/conf",
  //         "hadoop.home": "/opt/hadoop",
  //         "flush.size": "100",
  //         "rotate.interval.ms": "1000"
  //     },
  //     "tasks": [
  //         { "connector": "hdfs-sink-connector", "task": 1 },
  //         { "connector": "hdfs-sink-connector", "task": 2 },
  //         { "connector": "hdfs-sink-connector", "task": 3 }
  //     ]
  // }
  public Future<HttpResponse<Buffer>> createConnector(
    String arg,
    WebClient client
  )
    throws Throwable {
    System.out.println(
      "Number of threads in debezium client before running" +
      Thread.activeCount()
    );

    Settings settings = new Settings("development");
    StageInstance stage = settings.settings;
    URI uri = URI.create(stage.services.debezium.servers);
    String host = uri.getHost();
    Integer port = uri.getPort();

    Future<HttpResponse<Buffer>> res = client
      .post(port, host, "/connectors")
      .sendJsonObject(new JsonObject(arg));

    return res;
  }
}
