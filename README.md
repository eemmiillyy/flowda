## Development

#### Environment variables

Get the SECRET environment variable from the maintainer.

#### Start the services

```bash
docker-compose up -d
```

#### Build the job

```bash
cd flow-flink-job
mvn install
mvn -DskipTests=true clean package
```

#### Build the server

```bash
cd flow-core
export SECRET=[secret] STAGE=test
mvn install
mvn -DskipTests=true clean package
```

#### Upload the job to the cluster and start the server

```bash
export STAGE=development
`./setupFlink.sh`
```

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

## Testing

Tests use JUnit and Mockito. There are a combination of unit tests and integration tests. Each test suite for each domain should live beside the module they are testing.

```bash
cd flow-core
export SECRET=[secret] STAGE=test && mvn test

mvn -Dtest=[suiteName] test # Run a single test
# E.g. mvn -Dtest=CreateQueryEndpointTest test

mvn -Dtest=[suiteName]\#[methodName] test # Run a single method in a test (Remove escape character "\")
# E.g. mvn -Dtest=CreateQueryEndpointTest#testThrowsWithMissingConnectionStringQuery test
```

## Using Settings.json to encrypt new fields and decrypt existing fields

1. Inside `flow-core/src/main/resources/Settings.json`, Change the field (prefixed with `$$`) to the plaintext version of the new settings field you want to use
2. Edit `Server.java` to:

```java
   // ...
   import flow.core.Settings.Settings;
   // ...
    Settings settings = new Settings();
    settings.encrypt();
```

3. Build and run to generate the encrypted value

```bash
export SECRET=[secret] STAGE=[stage] && mvn package -Dmaven.test.skip
java -jar target/flow.core-1.0-SNAPSHOT.jar
```

3. Replace `Settings.json` values with encrypted ones. Right now all fields prefixed with `$$` inside of the stage block specified by the environment variable STAGE will be
   encrypted. If you want to encrypt only a single field at a time you can use `encryptField(String field)`

4. Edit `Server.java`:

```java
   // ...
   import flow.core.Settings.Settings;
   // ...
    Settings settings = new Settings();
    settings.decrypt();
```

```bash
java -jar target/flow.core-1.0-SNAPSHOT.jar
```

> Verify that the decrypted output was correct.

5. Undo all changes to server file and rebuild with new `Settings.json` fields (encrypted versions from step 3).

## Debug

```bash
kcat -b localhost:9093 -X security.protocol=SASL_PLAINTEXT -X sasl.mechanisms=SCRAM-SHA-256 -X sasl.username=emily -X sasl.password=bleepbloop -t emilytwo.inventory.custom_output_table_name
```

```bash
kcat -b localhost:9093 -X security.protocol=SASL_SSL -X sasl.mechanisms=SCRAM-SHA-256 -X sasl.username=emily -X sasl.password=bleepbloop -X ssl.ca.location=/private/etc/ssl/flowda/ca-cert -L
```
