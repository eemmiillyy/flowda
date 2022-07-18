package userSource;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;

import com.google.gson.Gson;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
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

  public Bootstrap() {
    this.settings = new Settings();
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

  protected JsonObject returnError(String message, int code) {
    return new JsonObject().put("message", message).put("code", code);
  }

  public void close() {
    this.server.close();
  }

  public Future<HttpServer> start() {
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
            context.json(returnError(e.getMessage(), 4000));
          }

          // Parse arguments into JSON for easier handling in resolver
          CreateConnectionInput args = g.fromJson(
            body.asJsonObject().toString(),
            CreateConnectionInput.class
          );

          // Check args are present
          Field[] fields = args.getClass().getFields();
          if (allFieldsPresent(fields, args).status == false) {
            String message =
              String.join(
                ",",
                allFieldsPresent(fields, args).missingFieldNames
              ) +
              " are missing.";
            context.json(returnError(message, 4001));
            return;
          }

          // Validate connection string
          try {
            ArgumentValidator validator = new ArgumentValidator();
            validator.validateConnectionString(args.connectionString);
          } catch (Throwable e) {
            context.json(returnError(e.getMessage(), 4002));
          }

          // Validate environment id
          try {
            ArgumentValidator validator = new ArgumentValidator();
            validator.validateStringInput(args.environmentId, "environmentId");
          } catch (Throwable e) {
            context.json(returnError(e.getMessage(), 4002));
            return;
          }
          URI connectionStringFormatted = URI.create(args.connectionString);
          String formatted = debeziumArtifactGenerator.connectionString(
            connectionStringFormatted,
            args.environmentId
          );

          // Create the kafka connector with Debezium REST Client
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
            context.json(returnError(e.getMessage(), 4003));
          }
        }
      );

    // Mount the handler for all incoming requests at every path and HTTP method
    router
      .route("/createQuery")
      .handler(
        context -> {
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

          Field[] fields = args.getClass().getFields();
          if (allFieldsPresent(fields, args).status == false) {
            String message =
              String.join(
                ",",
                allFieldsPresent(fields, args).missingFieldNames
              ) +
              " are missing.";
            System.out.println(message);
            context.json(returnError(message, 4001));
            return;
          }

          // Validate connection string
          try {
            ArgumentValidator validator = new ArgumentValidator();
            validator.validateConnectionString(args.connectionString);
          } catch (Throwable e) {
            context.json(returnError(e.getMessage(), 4002));
            return;
          }

          // Validate alphanumeric fields
          try {
            ArgumentValidator validator = new ArgumentValidator();
            validator.validateStringInput(args.environmentId, "environmentId");
            validator.validateStringInput(args.databaseName, "databaseName");
            validator.validateStringInput(args.tableName, "tableName");
            validator.validateStringInput(args.fieldName, "fieldName");
          } catch (Throwable e) {
            context.json(returnError(e.getMessage(), 4002));
            return;
          }

          // TODO authorisation with JWT to make sure they control the db.

          ApiKey apiKeyFactory = new ApiKey();
          String apiKeyForUser;
          try {
            apiKeyForUser = apiKeyFactory.create();
          } catch (Exception e) {
            context.json(returnError(e.getMessage(), 4004));
            return;
          }

          try {
            String rule = kafkaShellClient.createACLUser(
              args.environmentId,
              apiKeyForUser
            );
            kafkaShellClient.run(rule);
          } catch (Exception e) {
            context.json(returnError(e.getMessage(), 4005));
            return;
          }
          try {
            String rule = kafkaShellClient.createACLRuleConsumer(
              args.environmentId
            );
            kafkaShellClient.run(rule);
          } catch (Exception e) {
            context.json(returnError(e.getMessage(), 4005));
            return;
          }
          try {
            String rule = kafkaShellClient.createACLRule(args.environmentId); // Create kafka ACL for environmentId
            kafkaShellClient.run(rule);
          } catch (Exception e) {
            context.json(returnError(e.getMessage(), 4005));
            return;
          }

          // TODO delete ACL updates if there was an error

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
            context.json(returnError(e.getMessage(), 4006));
            return;
          }

          String agreggateString = flinkArtifactGenerator.createAgreggateQuery(
            args.tableName,
            args.fieldName
          );

          String sinkString = flinkArtifactGenerator.createSinkTable(
            args.databaseName,
            args.tableName,
            args.fieldName,
            args.environmentId
          );

          // The field to sum needs to be an integer.
          String validJSON = String.format(
            "{\"programArgsList\" : [\"--source\",\"%s\",\"--query\", \"%s\",\"--sink\",\"%s\",\"--table\",\"%s\"],\"parallelism\": 1}",
            sourceString,
            agreggateString,
            sinkString,
            args.tableName
          );

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
                    returnError("Issue launching generated flink job", 4007)
                  );
                }
              );
            // TODO get job status after in order to make sure it went through

          } catch (Throwable e) {
            context.json(
              returnError("Unexpected error during create query request", 4008)
            );
          }
        }
      );

    return server
      // Handle every request using the router
      .requestHandler(router)
      // Start listening
      .listen(8888)
      .onFailure(message -> System.out.println(message));
  }
}
