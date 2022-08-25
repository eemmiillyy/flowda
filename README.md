## Table of Contents

- [Development](#development)

- [Testing](#testing)

- [Benchmarking](#benchmarking)

- [Demo](#demo)

- [API Reference](#reference)

- [Settings](#settings)

- [Debug](#debug)

<a name="development"/>

## Development

#### 1. Set up environment variables

Get the SECRET environment variable from the maintainer.

#### 2. Start the services

```bash
docker-compose up -d
```

#### 3. Build the job

```bash
cd flow-flink-job
mvn install
mvn clean package
```

#### 4. Build the user service

```bash
cd flow-core
mvn -DskipTests=true install
mvn -DskipTests=true clean package
```

#### 5. Upload the job to the cluster and start the user service

```bash
export SECRET=[secret] STAGE=development
`./setupFlink.sh`
```

<a name="testing"/>

## Testing

Tests use JUnit and Mockito. There are a combination of unit tests and integration tests.

```bash
cd flow-core
export SECRET=[secret] STAGE=test && mvn test

mvn -Dtest=[suiteName] test # Run a single test
# E.g. mvn -Dtest=CreateQueryEndpointTest test

mvn -Dtest=[suiteName]\#[methodName] test # Run a single method in a test (Remove escape character "\")
# E.g. mvn -Dtest=CreateQueryEndpointTest#testThrowsWithMissingConnectionStringQuery test
```

<a name="benchmarking"/>

## Benchmarking

The benchmark suites require that the user service application is running in development mode. It makes use of the two mysql databases defined in the docker-compose file.

#### 1. Restart the docker services for a clean slate

```bash
docker-compose down
docker-compose up -d
```

#### 2. Start application server in development mode

```bash
./setupFlink.sh # If broken pipe shows in terminal, services are not ready. Wait a few seconds and re rerun
./setupConnector.sh # Sets up the simple query benchmark
# Change the environmentId in `setupConnector.sh` to "complex" and the connectionString in the first post request to "mysqltwo" in order to use the second database. Note that this has to match what is in `BenchmarkRUnner.java`
./setupConnector.sh -c # Sets up the complex query benchmark
```

#### 3. Build the benchmarking suite

```bash
cd flow-benchmark
mvn install
mvn clean verify
```

#### 4. Run the suite

```bash
cd flow-benchmark
java -jar target/benchmarks.jar
```

> Note that the connection strings for simple and complex benchmarks differ by their port insinde `BenchmarkRunner.java`. This is because the suite is running locally, with the first mysql database exposes port 3306 externally (A.K.A to the host machine), and the second mysql database exposes port 3307. Internally (A.K.A within the docker network), they both expose 3306 with only the container name differing (mysql versus mysqltwo).

<a name="demo"/>

## Running the client demo

This demo is set up to work with the default super user and the simple query job. It is designed to showcase how a user could use the API key and username from the user service's response to connect to the public Kafka cluster to display processed results in their own app.

A connector and corresponding job needs to be running in order for the demo to work. For this you can run `setupFlink.sh` which will create a connector. After that, you can launch the job with `setupConnector.sh`. If you run `setupConnector.sh` without a `-c` flag, the default simple query will be used. If you want to change the name of the `environmentId` make sure that you also change it inside `server/index.ts`.

> If you want to test out the user and API key the user service responds with, you can copy the `environmentId` and `apiKey` from `setupConnector.sh` response into `server/index.ts`.

#### Running the server (localhost:4000)

```bash
cd flow-demo-app
cd server
yarn
yarn ts-node index.ts # Edit clientId, apiKey, and environmentId if necessary, after starting the job.
```

#### Running the client (localhost:3000)

```bash
cd flow-demo-app
cd client
yarn
yarn start
```

<a name="reference"/>

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

<a name="settings"/>

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

4. Replace `Settings.json` values with encrypted ones. Right now all fields prefixed with `$$` inside of the stage block specified by the environment variable STAGE will be
   encrypted. If you want to encrypt only a single field at a time you can use `encryptField(String field)`

5. Edit `Server.java`:

```java
   // ...
   import flow.core.Settings.Settings;
   // ...
    Settings settings = new Settings();
    settings.decrypt();
```

6. Build and run to generate the encrypted value

```bash
export SECRET=[secret] STAGE=[stage] && mvn package -Dmaven.test.skip
java -jar target/flow.core-1.0-SNAPSHOT.jar
```

> Verify that the decrypted output was correct.

7. Undo all changes to server file and rebuild with new `Settings.json` fields (encrypted versions from step 3).

<a name="debug"/>

## Debug

These commands are helpful if you want to look directly at the kafka topic data.

```bash
kcat -b localhost:9093 -X security.protocol=SASL_PLAINTEXT -X sasl.mechanisms=SCRAM-SHA-256 -X sasl.username=emily -X sasl.password=bleepbloop -t simple.inventory.custom_output_table_name
```

```bash
kcat -b localhost:9093 -X security.protocol=SASL_SSL -X sasl.mechanisms=SCRAM-SHA-256 -X sasl.username=emily -X sasl.password=bleepbloop -X ssl.ca.location=/private/etc/ssl/flowda/ca-cert -L
```
