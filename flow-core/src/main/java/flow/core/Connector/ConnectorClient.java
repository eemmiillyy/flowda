package flow.core.Connector;

import java.net.URI;

import flow.core.Settings.Settings;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class ConnectorClient {

  private Settings settings;

  public ConnectorClient(Settings settings) {
    this.settings = settings;
  }

  public Future<HttpResponse<Buffer>> createConnector(
    String arg,
    WebClient client
  )
    throws Throwable {
    URI uri = URI.create(this.settings.settings.services.debezium.servers);
    String host = uri.getHost();
    Integer port = uri.getPort();
    Future<HttpResponse<Buffer>> res = client
      .post(port, host, "/connectors")
      .timeout(6000)
      .sendJsonObject(new JsonObject(arg));
    return res;
  }
}
