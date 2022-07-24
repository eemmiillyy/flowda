package flow.core.Job;

import java.net.URI;

import flow.core.Settings.Settings;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class JobClient {

  private Settings settings;

  public JobClient(Settings settings) {
    this.settings = settings;
  }

  public Future<HttpResponse<Buffer>> runJob(
    String body,
    WebClient client,
    String url
  )
    throws Throwable {
    URI uri = URI.create(this.settings.settings.services.flink.servers);
    String host = uri.getHost();
    Integer port = uri.getPort();

    Future<HttpResponse<Buffer>> res = client
      .post(port, host, url)
      .sendJsonObject(new JsonObject(body));
    return res;
  }
}
