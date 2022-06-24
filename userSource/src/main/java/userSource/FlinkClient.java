package userSource;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class FlinkClient {

  public Future<HttpResponse<Buffer>> runJob(
    String body,
    WebClient client,
    String url
  )
    throws Throwable {
    if (body.length() > 0) {
      Future<HttpResponse<Buffer>> res = client
        .post(8081, "localhost", url)
        .sendJsonObject(new JsonObject(body));
      return res;
    } else {
      // TODO configure method to be inputtable
      Future<HttpResponse<Buffer>> res = client
        .get(8081, "localhost", url)
        .send();
      return res;
    }
  }
}
