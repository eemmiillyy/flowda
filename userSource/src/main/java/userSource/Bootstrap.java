package userSource;

import java.lang.reflect.Field;
import java.util.ArrayList;

import com.google.gson.Gson;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import userSource.Connector.ConnectorClient;
import userSource.Connector.ConnectorResponseType;
import userSource.Connector.ConnectorSource;
import userSource.Connector.Kafka.KafkaClient;
import userSource.Job.JobClient;
import userSource.Job.JobResponseType;
import userSource.Job.JobSource;
import userSource.Settings.Settings;
import userSource.Utils.ApiKey;
import userSource.Utils.ArgumentValidator;
import userSource.Utils.ConnectionStringParser;
import userSource.Utils.ConnectionStringParser.ConnectionStringParsed;

public class Bootstrap {

  public Settings settings;
  public KafkaClient kafkaClient;
  public JobSource jobSource;
  public ConnectorSource connectorSource;
  public JobClient jobClient;
  public ConnectorClient connectorClient;
  public static WebClient client = WebClient.create(Vertx.vertx());
  io.vertx.core.http.HttpServer server;
  Vertx vertexInstance;
  Gson g;

  public Bootstrap() {
    this.settings = new Settings();
    this.kafkaClient = new KafkaClient(this.settings);
    this.jobSource = new JobSource(this.settings);
    this.connectorSource = new ConnectorSource(this.settings);
    this.jobClient = new JobClient(this.settings);
    this.connectorClient = new ConnectorClient(this.settings);
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
          String formatted = connectorSource.connectionString(
            args.connectionString,
            args.environmentId
          );

          // Create the kafka connector with Debezium REST Client
          try {
            this.connectorClient.createConnector(formatted, client)
              .onSuccess(
                result -> {
                  context.json(
                    new JsonObject()
                    .put(
                        "data",
                        result.bodyAsJson(ConnectorResponseType.class).name !=
                          null
                          ? result.bodyAsJson(ConnectorResponseType.class).name
                          : result.bodyAsJson(ConnectorResponseType.class)
                            .message
                      )
                  );
                }
              )
              .onFailure(
                handler -> {
                  context.json(
                    returnError(
                      "Unable to communicate with debezium service. It may be offline.",
                      4003
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
            validator.validateStringInput(args.sourceSql, "sourceSql"); // separate CREATE TABLE validation function
            validator.validateStringInput(args.querySql, "querySql"); // separate CREATE SELECT validation function
            validator.validateStringInput(args.sinkSql, "sinkSql"); // separate CREATE TABLE validation function
            if (args.sourceSqlTableTwo != null) {
              validator.validateStringInput(
                args.sourceSqlTableTwo, // separate CREATE TABLE validation function
                "sourceSqlTableTwo"
              );
            }
          } catch (Throwable e) {
            context.json(returnError(e.getMessage(), 4002));
            return;
          }
          // TODO make sure they own the database
          ApiKey apiKeyFactory = new ApiKey();
          String apiKeyForUser;
          try {
            apiKeyForUser = apiKeyFactory.create();
          } catch (Exception e) {
            context.json(returnError(e.getMessage(), 4004));
            return;
          }

          // TODO delete ACL updates if there was an error

          // Grab database name from connection string
          ConnectionStringParsed connectionInfo = new ConnectionStringParser()
          .parse(args.connectionString);

          String formattedSourceSql;
          String formattedSourceSqlTwo = null;
          try {
            // Grab tablename
            String tableName = jobSource.extractTableNameFromCreateStatement(
              args.sourceSql
            );
            formattedSourceSql =
              jobSource.appendKafkaConnectionInfo(
                false,
                args.sourceSql,
                connectionInfo.dbName,
                args.environmentId,
                tableName
              );

            if (args.sourceSqlTableTwo != null) {
              // Grab tablename
              String tableNameTwo = jobSource.extractTableNameFromCreateStatement(
                args.sourceSqlTableTwo
              );
              formattedSourceSqlTwo =
                jobSource.appendKafkaConnectionInfo(
                  false,
                  args.sourceSqlTableTwo,
                  connectionInfo.dbName,
                  args.environmentId,
                  tableNameTwo
                );
            }
          } catch (Throwable e) {
            context.json(returnError(e.getMessage(), 4006));
            return;
          }

          // Extract aggregate query table name
          String sinkTableName = jobSource.extractTableNameFromCreateStatement(
            args.sinkSql
          );
          String sinkString = jobSource.appendKafkaConnectionInfo(
            true,
            args.sinkSql,
            connectionInfo.dbName,
            args.environmentId,
            sinkTableName
          );

          // The field to sum needs to be an integer.
          String validJSON = String.format(
            "{\"programArgsList\" : [\"--source\",\"%s\", \"--sourceTwo\",\"%s\",\"--query\", \"%s\",\"--sink\",\"%s\",\"--table\",\"%s\"],\"parallelism\": 1}",
            formattedSourceSql,
            formattedSourceSqlTwo,
            args.querySql,
            sinkString,
            sinkTableName
          );

          System.out.println(validJSON);
          try {
            jobClient
              .runJob(
                validJSON,
                client,
                this.settings.settings.services.flink.jar
              )
              .onSuccess(
                response -> {
                  io.vertx.core.json.JsonObject res = new JsonObject()
                    .put("name", "successfully started Flink job.")
                    .put("environmentId", args.environmentId)
                    .put("apiKey", apiKeyForUser)
                    .put(
                      "jobId",
                      response.bodyAsJson(JobResponseType.class).jobid
                    );

                  // If all was successful then add permissions
                  if (
                    response.bodyAsJson(JobResponseType.class).jobid != null
                  ) {
                    try {
                      String rule = kafkaClient.createPermissions(
                        args.environmentId,
                        apiKeyForUser
                      );
                      kafkaClient.run(rule);
                    } catch (Exception e) {
                      context.json(returnError(e.getMessage(), 4005));
                      return;
                    }
                    context.json(res);
                  } else {
                    context.json(
                      returnError("Issue launching generated flink job", 4007)
                    );
                  }
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
      .onSuccess(
        server -> {
          System.out.println(
            "Application server started on port" + server.actualPort()
          );
        }
      )
      .onFailure(message -> System.out.println(message));
  }
}
