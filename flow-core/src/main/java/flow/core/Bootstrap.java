package flow.core;

import java.lang.reflect.Field;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;

import org.json.JSONException;

import com.google.gson.Gson;

import flow.core.Connector.ConnectorClient;
import flow.core.Connector.ConnectorResponseType;
import flow.core.Connector.ConnectorSource;
import flow.core.Errors.ClientErrorAuthorization;
import flow.core.Errors.ClientErrorInvalidInput;
import flow.core.Errors.ClientErrorJsonParseError;
import flow.core.Errors.ClientErrorMissingInput;
import flow.core.Errors.ErrorBase;
import flow.core.Errors.ServerErrorKafkaACLGeneration;
import flow.core.Errors.ServerErrorUnableToCreateDebeziumConnector;
import flow.core.Errors.ServerErrorUnableToCreateFlinkJob;
import flow.core.Job.JobClient;
import flow.core.Job.JobResponseType;
import flow.core.Job.JobSource;
import flow.core.Kafka.KafkaClient;
import flow.core.Settings.Settings;
import flow.core.Utils.ApiKey;
import flow.core.Utils.ArgumentValidator;
import flow.core.Utils.ConnectionStringParser;
import flow.core.Utils.ConnectionStringParser.ConnectionStringParsed;
import flow.core.Utils.JWT;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;

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

  // TODO generic function for errors
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
            context.json(
              new ClientErrorJsonParseError().toJson(e.getMessage())
            );
            return;
          }

          // Parse arguments into JSON for easier handling in resolver
          CreateConnectionInputType args = g.fromJson(
            body.asJsonObject().toString(),
            CreateConnectionInputType.class
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
            context.json(new ClientErrorMissingInput().toJson(message));
            return;
          }

          ArgumentValidator validator = new ArgumentValidator(this.settings);

          // Validate connection string
          try {
            validator.validateConnectionString(args.connectionString);
          } catch (Throwable e) {
            context.json(new ClientErrorInvalidInput().toJson(e.getMessage()));
            return;
          }

          // Validate environment id
          try {
            validator.validateStringInput(args.environmentId, "environmentId");
            validator.validateEnvironmentId(args.environmentId);
          } catch (Throwable e) {
            context.json(new ClientErrorInvalidInput().toJson(e.getMessage()));
            return;
          }
          String formatted;
          try {
            formatted =
              connectorSource.build(args.connectionString, args.environmentId);
            System.out.println(formatted);
          } catch (Exception e) {
            context.json(new ErrorBase().toJson(e.getMessage()));
            return;
          }

          // Create the kafka connector with Debezium REST Client
          try {
            this.connectorClient.createConnector(formatted, client)
              .onSuccess(
                result -> {
                  try {
                    context
                      .response()
                      .headers()
                      .add(
                        "Authorization",
                        "Bearer " + new JWT().create(args.environmentId)
                      );
                  } catch (
                    InvalidKeyException
                    | NoSuchAlgorithmException
                    | JSONException e
                  ) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                  }

                  if (
                    result.bodyAsJson(ConnectorResponseType.class).name != null
                  ) {
                    context.json(
                      new JsonObject()
                      .put(
                          "data",
                          result.bodyAsJson(ConnectorResponseType.class).name
                        )
                    );
                  } else {
                    context.response().headers().remove("Authorization");
                    context.json(
                      new JsonObject()
                      .put(
                          "data",
                          result.bodyAsJson(ConnectorResponseType.class).message
                        )
                    );
                  }
                }
              )
              .onFailure(
                handler -> {
                  context.response().headers().remove("Authorization");
                  context.json(
                    new ServerErrorUnableToCreateDebeziumConnector().toJson("")
                  );
                }
              );
          } catch (Throwable e) {
            context.json(
              new ServerErrorUnableToCreateDebeziumConnector()
              .toJson(e.getMessage())
            );
          }
        }
      );

    // Mount the handler for all incoming requests at every path and HTTP method
    router
      .route("/createQuery")
      .handler(
        context -> {
          io.vertx.ext.web.RequestBody body = null;
          String environmentId = "";
          try {
            body = context.body();
            // Extract header
            System.out.println(context.request().getHeader("Authorization"));
            try {
              environmentId =
                new JWT()
                .decodeJWT(
                    context.request().getHeader("Authorization").substring(7)
                  )
                  .environmentId;
            } catch (Throwable e) {
              context.json(
                new ClientErrorAuthorization().toJson(e.getMessage())
              );
              return;
            }
          } catch (Throwable e) {
            context.json(new ErrorBase().toJson(e.getMessage()));
            return;
          }
          // Parse arguments into JSON for easier handling in resolver
          CreateQueryInputType args = g.fromJson(
            body.asJsonObject().toString(),
            CreateQueryInputType.class
          );

          Field[] fields = args.getClass().getFields();
          if (allFieldsPresent(fields, args).status == false) {
            String message =
              String.join(
                ",",
                allFieldsPresent(fields, args).missingFieldNames
              ) +
              " are missing.";
            context.json(new ClientErrorMissingInput().toJson(message));
            return;
          }

          ArgumentValidator validator = new ArgumentValidator(this.settings);

          // Validate connection string
          try {
            validator.validateConnectionString(args.connectionString);
          } catch (Throwable e) {
            context.json(new ClientErrorInvalidInput().toJson(e.getMessage()));
            return;
          }
          // Validate alphanumeric fields
          try {
            validator.validateStringInput(args.sourceSql, "sourceSql"); // separate CREATE TABLE validation function
            validator.validateStringInput(
              args.sourceSqlTableTwo, // separate CREATE TABLE validation function
              "sourceSqlTableTwo"
            );
            validator.validateStringInput(args.querySql, "querySql"); // separate CREATE SELECT validation function
            validator.validateStringInput(args.sinkSql, "sinkSql"); // separate CREATE TABLE validation function
          } catch (Throwable e) {
            context.json(new ClientErrorInvalidInput().toJson(e.getMessage()));
            return;
          }

          ApiKey apiKeyFactory = new ApiKey();
          String apiKeyForUser;
          try {
            apiKeyForUser = apiKeyFactory.create();
          } catch (Exception e) {
            context.json(new ErrorBase().toJson(e.getMessage()));
            return;
          }

          // TODO delete ACL updates if there was an error

          // Grab database name from connection string
          ConnectionStringParsed connectionInfo = new ConnectionStringParser()
          .parse(args.connectionString);
          String formattedSourceSql;
          String formattedSourceSqlTwo;
          try {
            // Grab tablename
            String tableName = jobSource.extractTableNameFromCreateStatement(
              args.sourceSql
            );
            System.out.println(tableName);
            formattedSourceSql =
              jobSource.build(
                true,
                args.sourceSql,
                connectionInfo.dbName,
                environmentId,
                tableName
              );
            System.out.println(formattedSourceSql);

            // Grab tablename
            String tableNameTwo = jobSource.extractTableNameFromCreateStatement(
              args.sourceSqlTableTwo
            );
            formattedSourceSqlTwo =
              jobSource.build(
                true,
                args.sourceSqlTableTwo,
                connectionInfo.dbName,
                environmentId,
                tableNameTwo
              );
            System.out.println(formattedSourceSqlTwo);
          } catch (Throwable e) {
            context.json(new ErrorBase().toJson(e.getMessage()));
            return;
          }

          // Extract aggregate query table name
          String sinkTableName = jobSource.extractTableNameFromCreateStatement(
            args.sinkSql
          );
          String sinkString;
          try {
            sinkString =
              jobSource.build(
                false,
                args.sinkSql,
                connectionInfo.dbName,
                environmentId,
                sinkTableName
              );
            System.out.println(sinkString);
          } catch (Exception e) {
            context.json(new ErrorBase().toJson(e.getMessage()));
            return;
          }

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
                  String environmentIdLocal;
                  try {
                    // TODO do not recreate variable beacuse of lambda
                    System.out.println(
                      "fromsuccess" +
                      context.request().getHeader("Authorization").substring(7)
                    );
                    environmentIdLocal =
                      new JWT()
                      .decodeJWT(
                          context
                            .request()
                            .getHeader("Authorization")
                            .substring(7)
                        )
                        .environmentId;
                  } catch (
                    InvalidKeyException
                    | NoSuchAlgorithmException
                    | JSONException
                    | ParseException e
                  ) {
                    context.json(
                      new ServerErrorUnableToCreateFlinkJob()
                      .toJson(e.getMessage())
                    );
                    return;
                  }

                  io.vertx.core.json.JsonObject res = new JsonObject()
                    .put("name", "successfully started Flink job.")
                    .put("environmentId", environmentIdLocal)
                    .put("apiKey", apiKeyForUser)
                    .put(
                      "jobId",
                      response.bodyAsJson(JobResponseType.class).jobid
                    );

                  // If all was successful then add permissions in background thread
                  if (
                    response.bodyAsJson(JobResponseType.class).jobid != null
                  ) {
                    vertexInstance.executeBlocking(
                      call -> {
                        try {
                          String rule = kafkaClient.createPermissions(
                            environmentIdLocal,
                            apiKeyForUser
                          );
                          kafkaClient.modifyACL(rule);
                          call.complete();
                        } catch (Exception e) {
                          context.json(
                            new ServerErrorKafkaACLGeneration()
                            .toJson(e.getMessage())
                          );
                          return;
                        }
                      }
                    );

                    context.json(res);
                    return;
                  } else {
                    context.json(
                      new ServerErrorUnableToCreateFlinkJob()
                      .toJson(response.bodyAsString())
                    );
                    return;
                  }
                }
              )
              .onFailure(
                error -> {
                  System.out.println(error);
                  context.json(
                    new ServerErrorUnableToCreateFlinkJob()
                    .toJson(error.getMessage())
                  );
                }
              );
            // TODO get job status after in order to make sure it went through

          } catch (Throwable e) {
            context.json(
              new ServerErrorUnableToCreateFlinkJob().toJson(e.getMessage())
            );
          }
        }
      );

    return server
      .requestHandler(router)
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
