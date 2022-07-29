## Development

#### Start the services

```bash
docker-compose up -d
```

#### Build the job

```bash
cd flink-flow-job
mvn install
export SECRET=Chv1ocZ74xL9fl4hcJRfEvt6ZHmF6KlS1P6cDQBFdmjXOlwBJK0EAiYR1bdyzxVH STAGE=development && mvn -DskipTests=true clean package
```

#### Upload the job

1. Visit `localhost:8081/submit`

2. Upload .jar file `flow.flink.job-1.0-SNAPSHOT.jar`

3. Visit `http://0.0.0.0:8081/jars`

4. Extract the id of the jar.

5. Change the settings.[STAGE].services.flink.jar value to `/jars/[XXXXX]/run`

**NOTE** We must manually upload the packaged .jar file from these steps to flink via the UI and retrieve it's job id. After that `Settings.json` jar file path needs to be updated.

#### Build the server

```bash
cd flow-core
mvn install
export SECRET=[secret] STAGE=test && mvn clean package
```

#### Running the server

```bash
export SECRET=[secret] STAGE=development
java -jar target/flow.core-1.0-SNAPSHOT.jar
```

## Test

Tests use JUnit and Mockito. There are a combination of unit tests and integration tests. Each test suite for each domain should live beside the module they are testing.
`export SECRET=[secret] STAGE=test && mvn test && unset SECRET STAGE` to check the services are healthy

Run a single test
`mvn -Dtest=AppTest test`

Run a single test method
`mvn -Dtest=AppTest#methodname test`

## API Reference

`/createConnection`

Verb: `POST`

Response Code: `200`

Creates a debezium/kafka connector for the given database. Introspects
the database and creates a topic in the kafka cluster with `environmentId.dbName`. There is no check for whether the database is reachable. That needs to be manually ensured. There is also no check to the permissions. Needs to be manually run. Returns a JWT that is set as a cookie to be used for the next request.

_Request_

```json
{
  "connectionString": "mysql://user:pass@mysql:3306/dbname",
  "environmentId": "uniqueIdToUSeAsKafkaTopic"
}
```

_Response_

```json
{
  "data": "uniqueIdToUSeAsKafkaTopic-connector"
}
```

> A Json Web Token will be returned in the response header and needs to be used as the Authorization: Bearer <token> in the next request.

---

`/createQuery`

Verb: `POST`

Response Code: `200`

Launches a flink job with the given source, aggregate, and sink table. The result of the aggregate will stream to the environmentId.dbName.outputTableName topic. db name inferred from connection string. tablename will be inferred from source statement. agreggateSql tablename needs to match the source. output table name will be inferred from the sink statement. Returns an access token that will be the users password for the kafka ACL for
the topic. DO NOT add semi colons to end of the input

_Request_

```json
{
  "connectionString": "mysql://user:pass@mysql:3306/inventory",
  "sourceSql": "CREATE TABLE products_on_hand (quantity INT, product_id INT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)",
  "sourceSqlTableTwo": "CREATE TABLE orders (order_number BIGINT, purchaser BIGINT, quantity BIGINT, product_id BIGINT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL,  WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)",
  "querySql": "SELECT SUM(quantity) as summed FROM products_on_hand",
  "sinkSql": "CREATE TABLE custom_output_table_name (summed INT)"
}
```

_Response_

```json
{
  "name": "successfully started Flink job.",
  "environmentId": "XXXX",
  "ApiKey": "XXXX",
  "jobId": "XXXX"
}
```

## Error Codes

| 4000                                                                                    | Unable to parse body of request. Needs to be JSON.                             |
| --------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| 4001                                                                                    | Missing user input.                                                            |
| --------------------------------------------------------------------------------------- |
| 4002                                                                                    | User input validation error.                                                   |
| --------------------------------------------------------------------------------------- |
| 4003                                                                                    | Unable to communicate with debezium                                            |
| --------------------------------------------------------------------------------------- |
| 4004                                                                                    | API key generation issue                                                       |
| --------------------------------------------------------------------------------------- |
| 4005                                                                                    | Kafka ACL rule creation issue                                                  |
| --------------------------------------------------------------------------------------- |
| 4006                                                                                    | Flink artefact generation issue. May be the result of faulty kafka connection. |
| --------------------------------------------------------------------------------------- |
| 4007                                                                                    | Issue running generated Flink job                                              |
| --------------------------------------------------------------------------------------- |
| 4008                                                                                    | Unexpected/unhandled error during request                                      |
| --------------------------------------------------------------------------------------- |
| 4009                                                                                    | Issue connecting to the database. Bad connection string or privileges.         |
| --------------------------------------------------------------------------------------- |

## Debug

```bash
kcat -b localhost:9093 -X security.protocol=SASL_PLAINTEXT -X sasl.mechanisms=SCRAM-SHA-256 -X sasl.username=emily -X sasl.password=bleepbloop= -L
```

## Re-encrypting a phrase

1. Change the field (prefixed with `$$`) to the plaintext version
2. Edit `Server.java` to:

```java
    Settings settings = new Settings(System.getenv("STAGE"));
    settings.encrypt();
    settings.decrypt();
```

3. Run

```bash
export SECRET=[secret] STAGE=development && mvn package -Dmaven.test.skip && unset SECRET STAGE
SECRET=[secret] STAGE=development java -jar target/flow.core-1.0-SNAPSHOT.jar
```

**NOTE** Decryption will fail during this since it will try to decrypt the plaintext which will throw an error. That is expected.
**NOTE** If the stage passwords are different you need to

3. Replace `Settings.json` values with encrypted ones.
   **NOTE** right now all fields prefixed with `$$` inside of each specified stage will be
   encrypted and decrypted with the command from step 2.
   If you want to encrypt or decrypt only a single field at a time you can use `encryptField(String field)` or `decryptField(String field)`

4. Make sure it works with

```bash
SECRET=[secret] STAGE=development java -jar target/flow.core-1.0-SNAPSHOT.jar
```

Then undo changes to server and re run with new secret.
