package userSource.Flink;

import java.net.URI;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import userSource.Settings.Settings;
import userSource.Settings.SettingsShape.Stage.StageInstance;

public class FlinkClient {

  public Future<HttpResponse<Buffer>> runJob(
    String body,
    WebClient client,
    String url
  )
    throws Throwable {
    Settings settings = new Settings("development");
    StageInstance stage = settings.settings;
    URI uri = URI.create(stage.services.flink.servers);
    String host = uri.getHost();
    Integer port = uri.getPort();

    if (body.length() > 0) {
      Future<HttpResponse<Buffer>> res = client
        .post(port, host, url)
        .sendJsonObject(new JsonObject(body));
      return res;
    } else {
      // TODO configure method to be inputtable
      Future<HttpResponse<Buffer>> res = client.get(port, host, url).send();
      return res;
    }
  }
}
