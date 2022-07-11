package userSource;

import java.lang.reflect.Field;
import java.util.ArrayList;

import com.google.gson.Gson;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import userSource.Debezium.DebeziumArtifactGenerator;
import userSource.Debezium.DebeziumClient;
import userSource.Debezium.DebeziumResponseShape;
import userSource.Flink.FlinkArtifactGenerator;
import userSource.Flink.FlinkClient;
import userSource.Flink.FlinkResponseShape;
import userSource.Kafka.KafkaShellClient;
import userSource.Settings.Settings;
import userSource.Utils.ApiKey;
import userSource.Utils.ArgumentValidator;

public class Bootstrap {

  public Settings settings;
  public KafkaShellClient kafkaShellClient;
  public FlinkArtifactGenerator flinkArtifactGenerator;
  public DebeziumArtifactGenerator debeziumArtifactGenerator;
  public FlinkClient flinkClient;
  public DebeziumClient debeziumClient;
  public static WebClient client = WebClient.create(Vertx.vertx());
  io.vertx.core.http.HttpServer server;
  Vertx vertexInstance;
  Gson g;

  public Bootstrap(String stage) {
    this.settings = new Settings(stage);
    this.kafkaShellClient = new KafkaShellClient(this.settings);
    this.flinkArtifactGenerator = new FlinkArtifactGenerator(this.settings);
    this.debeziumArtifactGenerator =
      new DebeziumArtifactGenerator(this.settings);
    this.flinkClient = new FlinkClient(this.settings);
    this.debeziumClient = new DebeziumClient(this.settings);
  }

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
          System.out.println(args);
          // Check args are present
          // TODO return the error status code
          // TODO this only accepts {} right now, make sure now JSON object is also accepted
          Field[] fields = args.getClass().getFields();
          if (allFieldsPresent(fields, args).status == false) {
            String message =
              String.join(
                ",",
                allFieldsPresent(fields, args).missingFieldNames
              ) +
              " are missing.";
            System.out.println(message);

            context.json(
              new JsonObject().put("message", message).put("code", 4001)
            );
            return;
          }

          // Validate args do not pass length or contain bad characters
          try {
            ArgumentValidator validator = new ArgumentValidator();
            validator.validateConnectionString(args.connectionString);
          } catch (Throwable e) {
            context.json(new JsonObject().put("message", e).put("code", 4002));
          }

          // Format connection string for debezium
          // TODO validate the connection string so this never throws

          String formatted = debeziumArtifactGenerator.connectionString(
            args.connectionString,
            args.environmentId
          );

          // Create the kafka connector with REST Client
          try {
            this.debeziumClient.createConnector(formatted, client)
              .onSuccess(
                result -> {
                  System.out.println(result.bodyAsString());
                  System.out.println(
                    result.bodyAsJson(DebeziumResponseShape.class)
                  );
                  context.json(
                    new JsonObject()
                    .put(
                        // TODO return a bad status code for the user instead of 200
                        "data",
                        result.bodyAsJson(DebeziumResponseShape.class).name !=
                          null
                          ? result.bodyAsJson(DebeziumResponseShape.class).name
                          : result.bodyAsJson(DebeziumResponseShape.class)
                            .message
                      )
                  );
                }
              );
          } catch (Throwable e) {
            System.out.println(e.toString());
            context.json(new JsonObject().put("message", e).put("code", 4002));
          }
        }
      );

    // Mount the handler for all incoming requests at every path and HTTP method
    router
      .route("/createQuery")
      .handler(
        context -> {
          // Get the query parameter "name"
          io.vertx.ext.web.RequestBody body = null;
          try {
            body = context.body();
          } catch (Throwable e) {
            context.json(new JsonObject().put("message", e).put("code", 4000));
            return;
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
            System.out.println(message);
            context.json(
              new JsonObject().put("message", message).put("code", 4001)
            );
            return;
          }

          // TODO authorisation with JWT to make sure they control the db.

          // TODO Abstract this into Kafka source module
          // Create access token for user
          ApiKey apiKeyFactory = new ApiKey();
          String apiKeyForUser;
          try {
            apiKeyForUser = apiKeyFactory.create();
          } catch (Exception e) {
            context.json(new JsonObject().put("error", e.getMessage()));
            return;
          }

          // Create kafka user for environmentId/accessToken
          try {
            String rule = kafkaShellClient.createACLUser(
              args.environmentId,
              apiKeyForUser
            );
            kafkaShellClient.run(rule);
          } catch (Exception e) {
            context.json(new JsonObject().put("error", e.getMessage()));
            return;
          }
          // Group
          try {
            String rule = kafkaShellClient.createACLRuleConsumer(
              args.environmentId
            ); // Create kafka ACL for environmentId
            kafkaShellClient.run(rule);
          } catch (Exception e) {
            System.out.println(e);

            context.json(new JsonObject().put("error", e.getMessage()));
            return;
          }

          // Create kafka ACL for user and topic
          try {
            String rule = kafkaShellClient.createACLRule(args.environmentId); // Create kafka ACL for environmentId
            kafkaShellClient.run(rule);
          } catch (Exception e) {
            context.json(new JsonObject().put("error", e.getMessage()));
            return;
          }

          // // Create kafka client with the kafka user that has access to read/write to environmentId.*
          // // If there was an error at any point, delete the ACL for the user.

          // // TODO input validation
          // Generate arguments for flink job
          // TODO abstract into own Aritfact Source module
          String sourceString;
          try {
            sourceString =
              flinkArtifactGenerator.createSourceTable(
                args.databaseName,
                args.tableName,
                args.environmentId
              );

            System.out.println(sourceString);
          } catch (Throwable e) {
            context.json(new JsonObject().put("error", e.getMessage()));
            return;
          }

          String agreggateString = flinkArtifactGenerator.createAgreggateQuery(
            args.tableName,
            args.fieldName
          );
          System.out.println("AGG......." + agreggateString);

          String sinkString = flinkArtifactGenerator.createSinkTable(
            args.databaseName,
            args.tableName,
            args.fieldName,
            args.environmentId
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

          try {
            flinkClient
              .runJob(
                validJSON,
                client,
                this.settings.settings.services.flink.jar
              )
              .onSuccess(
                response -> {
                  System.out.println(response.body());
                  context.json(
                    new JsonObject()
                      .put("name", "successfully started Flink job.")
                      .put("environmentId", args.environmentId)
                      .put("apiKey", apiKeyForUser)
                      .put(
                        "jobId",
                        response.bodyAsJson(FlinkResponseShape.class).jobid
                      )
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
          FlinkClient flinkClient = new FlinkClient(this.settings);

          try {
            flinkClient
              .runJob("", client, "/jobs/" + args.jobId)
              .onSuccess(
                response -> {
                  context.json(new JsonObject().put("name", response.body()));
                }
              )
              .onFailure(
                error -> {
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
            "HTTP server started on port " +
            this.settings.settings.services.debezium.servers
          )
      );
  }
}
