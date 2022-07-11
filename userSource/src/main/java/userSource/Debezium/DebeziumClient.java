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

  Settings settings;

  public DebeziumClient(Settings settings) {
    this.settings = settings;
  }

  public Future<HttpResponse<Buffer>> createConnector(
    String arg,
    WebClient client
  )
    throws Throwable {
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
