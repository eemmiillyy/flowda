package flow.core.Errors;

import io.vertx.core.json.JsonObject;

/**
 * 4999
 * "Unknown issue, issue formatting or generating inputs."
 */
public class ErrorBase extends Error {

  public int code = 4999;
  public String message =
    "Unknown issue, issue formatting or generating inputs";

  public JsonObject toJson(String augmentMessage) {
    return new JsonObject()
      .put("message", this.message + ":" + augmentMessage)
      .put("code", this.code);
  }
}
