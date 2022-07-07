package userSource;

import java.lang.reflect.Field;
import java.util.ArrayList;

import com.google.gson.Gson;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import userSource.Debezium.DebeziumArtifactGenerator;
import userSource.Debezium.DebeziumClient;
import userSource.Flink.FlinkArtifactGenerator;
import userSource.Flink.FlinkClient;
import userSource.Kafka.KafkaShellClient;
import userSource.Settings.Settings;
import userSource.Settings.SettingsShape.Stage.StageInstance;
import userSource.Utils.ApiKey;
import userSource.Utils.ArgumentValidator;

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

          // TODO authorisation with JWT to make sure they control the db.

          // TODO Abstract this into Kafka source module
          // Create access token for user
          ApiKey apiKeyFactory = new ApiKey();
          String apiKeyForUser;
          try {
            apiKeyForUser = apiKeyFactory.create();
            System.out.println(apiKeyForUser);
          } catch (Exception e) {
            context.json(new JsonObject().put("error", e.getMessage()));
            return;
          }

          // Create kafka user for environmentId/accessToken
          KafkaShellClient kafkaShellClient = new KafkaShellClient();
          try {
            kafkaShellClient.addACLUser(args.environmentId, apiKeyForUser);
          } catch (Exception e) {
            context.json(new JsonObject().put("error", e.getMessage()));
            return;
          }

          // Group
          try {
            kafkaShellClient.addACLRuleConsumer(args.environmentId); // Create kafka ACL for environmentId
          } catch (Exception e) {
            context.json(new JsonObject().put("error", e.getMessage()));
            return;
          }

          // Create kafka ACL for user and topic
          try {
            kafkaShellClient.addACLRule(args.environmentId); // Create kafka ACL for environmentId
          } catch (Exception e) {
            context.json(new JsonObject().put("error", e.getMessage()));
            return;
          }

          // // Create kafka client with the kafka user that has access to read/write to environmentId.*
          // // If there was an error at any point, delete the ACL for the user.

          // // TODO input validation
          // Generate arguments for flink job
          // TODO abstract into own Aritfact Source module
          FlinkArtifactGenerator sourceTable = new FlinkArtifactGenerator(
            args.environmentId
          );
          String sourceString;
          try {
            sourceString =
              sourceTable.createSourceTable(args.databaseName, args.tableName);

            System.out.println("SOURCE......." + sourceString);
          } catch (Throwable e) {
            context.json(new JsonObject().put("error", e.getMessage()));
            return;
          }

          String agreggateString = sourceTable.createAgreggateQuery(
            args.tableName,
            args.fieldName
          );
          System.out.println("AGG......." + agreggateString);

          String sinkString = sourceTable.createSinkTable(
            args.databaseName,
            args.tableName,
            args.fieldName
          );
          System.out.println("SINK......." + sinkString);

          // The field to sum needs to be an integer.
          String validJSON = String.format(
            "{\"programArgsList\" : [\"--source\",\"%s\",\"--query\", \"%s\",\"--sink\",\"%s\",\"--table\",\"%s\"],\"parallelism\": 1}",
            sourceString,
            agreggateString,
            sinkString,
            args.tableName
          );
          System.out.println("VALID......." + validJSON);

          FlinkClient flinkClient = new FlinkClient();

          System.out.println("FLINK CLIENT CREATED.......");

          try {
            Settings settings = new Settings("development");
            StageInstance stage = settings.settings;

            flinkClient
              .runJob(validJSON, client, stage.services.flink.jar)
              .onSuccess(
                response -> {
                  System.out.println("SUCCESS.......");
                  // TODO send the job id back to user so they can check the status with it
                  System.out.println(response.body());
                  context.json(
                    new JsonObject()
                      .put("name", "successfully started Flink job.")
                      .put("environmentId", args.environmentId)
                      .put("apiKey", apiKeyForUser)
                      .put("jobId", response.body()) // TODO extract on job id
                  );
                }
              )
              .onFailure(
                error -> {
                  System.out.println("FAILED.......");
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
