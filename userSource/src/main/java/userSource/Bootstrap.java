package userSource;

import com.google.gson.Gson;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class Bootstrap {

  public static WebClient client = WebClient.create(Vertx.vertx());

  public class ClientError {

    Number code;
    String Message;
  }

  public class CreateConnectionInput {

    public String connectionString;
    public String environmentId;
  }

  public class CheckJobStatusInput {

    public String jobId;
  }

  public class CreateQueryInput {

    public String connectionString; // TODO Switch this to JWT in header
    public String environmentId; // or dbServerName, encode in JWT
    public String databaseName;
    public String tableName;
    public String fieldName;
  }

  io.vertx.core.http.HttpServer server;
  Vertx vertexInstance;
  Gson g;

  public class AllFieldsPresentOutput {

    public Boolean status = true;
    public ArrayList<String> missingFieldNames = new ArrayList<String>();
  }

  public <T> AllFieldsPresentOutput allFieldsPresent(
    Field[] classFields,
    T args
  ) {
    AllFieldsPresentOutput returning = new AllFieldsPresentOutput();
    for (Field field : classFields) {
      try {
        Object value = field.get(args);
        if (value == null) {
          returning.status = false;
          returning.missingFieldNames.add(field.getName());
        }
      } catch (Throwable e) {}
    }
    return returning;
  }

  public void start() {
    vertexInstance = Vertx.vertx();
    server = vertexInstance.createHttpServer();
    g = new Gson();

    KafkaClient kafka = new KafkaClient();
    // Create a Router
    Router router = Router.router(vertexInstance);
    router.route().handler(BodyHandler.create());

    router
      .route("/createConnection")
      .handler(
        context -> {
          System.out.println(
            "Number of threads in createConnection" + Thread.activeCount()
          );
          io.vertx.ext.web.RequestBody body = null;
          try {
            body = context.body();
          } catch (Throwable e) {
            context.json(new JsonObject().put("message", e).put("code", 4000));
          }

          // Parse arguments into JSON for easier handling in resolver
          CreateConnectionInput args = g.fromJson(
            body.asJsonObject().toString(),
            CreateConnectionInput.class
          );

          // Check args are present
          // TODO return the error status code
          Field[] fields = args.getClass().getFields();
          if (allFieldsPresent(fields, args).status == false) {
            String message =
              String.join(
                ",",
                allFieldsPresent(fields, args).missingFieldNames
              ) +
              " are missing.";
            context.json(
              new JsonObject().put("message", message).put("code", 4001)
            );
          }

          // Validate args do not pass length or contain bad characters
          try {
            ArgumentValidator validator = new ArgumentValidator();
            validator.validateConnectionString(args.connectionString);
          } catch (Throwable e) {
            context.json(new JsonObject().put("message", e).put("code", 4002));
          }

          // Format connection string for debezium
          DebeziumArtifactGenerator debezium = new DebeziumArtifactGenerator();
          String formatted = debezium.connectionString(
            args.connectionString,
            args.environmentId
          );

          // Create the kafka connector with REST Client
          try {
            DebeziumClient debeziumClient = new DebeziumClient();
            Future<HttpResponse<Buffer>> res = debeziumClient.createConnector(
              formatted,
              client
            );

            res.onSuccess(
              result -> {
                context.json(
                  new JsonObject().put("data", result.bodyAsString())
                );
              }
            );
          } catch (Throwable e) {
            context.json(new JsonObject().put("message", e).put("code", 4002));
          }
        }
      );

    // Mount the handler for all incoming requests at every path and HTTP method
    router
      .route("/createQuery")
      .handler(
        context -> {
          System.out.println(
            "Number of threads in createQuery" + Thread.activeCount()
          );
          // Get the query parameter "name"
          io.vertx.ext.web.RequestBody body = null;
          try {
            body = context.body();
          } catch (Throwable e) {
            context.json(new JsonObject().put("message", e).put("code", 4000));
          }

          // Parse arguments into JSON for easier handling in resolver
          CreateQueryInput args = g.fromJson(
            body.asJsonObject().toString(),
            CreateQueryInput.class
          );

          // Check args are present
          // TODO return the error status code
          Field[] fields = args.getClass().getFields();
          if (allFieldsPresent(fields, args).status == false) {
            String message =
              String.join(
                ",",
                allFieldsPresent(fields, args).missingFieldNames
              ) +
              " are missing.";
            context.json(
              new JsonObject().put("message", message).put("code", 4001)
            );
          }

          // TODO authorisation

          // TODO input validation

          // Generate arguments for flink job
          FlinkArtifactGenerator sourceTable = new FlinkArtifactGenerator(
            kafka
          );
          String sourceString;
          try {
            sourceString =
              sourceTable.createSourceTable(
                args.environmentId,
                args.databaseName,
                args.tableName
              );
          } catch (Throwable e) {
            context.json(new JsonObject().put("name", e.getMessage()));
            return;
          }

          String agreggateString = sourceTable.createAgreggateQuery(
            args.tableName,
            args.fieldName
          );

          String sinkString = sourceTable.createSinkTable(
            args.environmentId,
            args.databaseName,
            args.tableName,
            args.fieldName
          );

          // The field to sum needs to be an integer.
          String validJSON = String.format(
            "{\"programArgsList\" : [\"--source\",\"%s\",\"--query\", \"%s\",\"--sink\",\"%s\",\"--table\",\"%s\"],\"parallelism\": 1}",
            sourceString,
            agreggateString,
            sinkString,
            args.tableName
          );
          FlinkClient flinkClient = new FlinkClient();

          try {
            flinkClient
              .runJob(
                validJSON,
                client,
                "/jars/520bd0c3-25fb-4a47-8d0a-9ae7a0a60b3e_processingSource-1.0-SNAPSHOT.jar/run"
              )
              .onSuccess(
                response -> {
                  // TODO send the job id back to user so they can check the status with it
                  System.out.println(response.body());
                  context.json(
                    new JsonObject()
                    .put("name", "successfully started Flink job.")
                  );
                }
              )
              .onFailure(
                error -> {
                  System.out.println(error);
                  context.json(
                    new JsonObject().put("error", "error launching flink job.")
                  );
                }
              );
            // TODO get job status after in order to make sure it went through

          } catch (Throwable e) {
            context.json(new JsonObject().put("name", e));
          }
        }
      );
    router
      .route("/checkJobStatus")
      .handler(
        context -> {
          // Get the query parameter "name"
          io.vertx.ext.web.RequestBody body = null;
          try {
            body = context.body();
          } catch (Throwable e) {
            context.json(new JsonObject().put("message", e).put("code", 4000));
          }

          // Parse arguments into JSON for easier handling in resolver
          CheckJobStatusInput args = g.fromJson(
            body.asJsonObject().toString(),
            CheckJobStatusInput.class
          );

          // Check args are present
          // TODO return the error status code
          Field[] fields = args.getClass().getFields();
          if (allFieldsPresent(fields, args).status == false) {
            String message =
              String.join(
                ",",
                allFieldsPresent(fields, args).missingFieldNames
              ) +
              " are missing.";
            context.json(
              new JsonObject().put("message", message).put("code", 4001)
            );
          }
          FlinkClient flinkClient = new FlinkClient();

          try {
            flinkClient
              .runJob("", client, "/jobs/" + args.jobId)
              .onSuccess(
                response -> {
                  // TODO send the job id back to user so they can check the status with it
                  System.out.println("success");
                  System.out.println(response.body());
                  context.json(new JsonObject().put("name", response.body()));
                }
              )
              .onFailure(
                error -> {
                  System.out.println("here" + error);
                  context.json(
                    new JsonObject().put("error", "error checking flink job.")
                  );
                }
              );
          } catch (Throwable e) {
            context.json(new JsonObject().put("name", e));
          }
        }
      );
    server
      // Handle every request using the router
      .requestHandler(router)
      // Start listening
      .listen(8888)
      .onFailure(message -> System.out.println(message))
      // Print the port
      .onSuccess(
        server ->
          System.out.println(
            "HTTP server started on port " + server.actualPort()
          )
      );
  }
}
