## Table of Contents

- [Development](#development)

- [Testing](#testing)

- [Benchmarking](#benchmarking)

- [Demo](#demo)

- [API Reference](#reference)

- [Settings](#settings)

- [Debug](#debug)

- [Deployment](#deployment)

<a name="development"/>

## Development

#### 1. Set up environment variables

Get the `SECRET` environment variable from the maintainer.
Get the `KAFKA_USER`environment variable from the maintainer.
Get the `KAFKA_PASSWORD`environment variable from the maintainer.

Create a .env with `KAFKA_USER` & `KAFKA_PASSWORD`. This file needs to exist because that is what docker uses when starting the services. These variables should also be exported to the console when running any application outisde of `flow-core`, so `flow-benchmark` and `flow-demo-app`.

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

> Before doing this, make sure `STAGE`, `SECRET`, `KAFKA_USER` & `KAFKA_PASSWORD` have been exported in the console.

```bash
docker-compose down
docker-compose up -d
```

#### 2. Start application server in development mode

```bash
./setupFlink.sh # If broken pipe shows in terminal, services are not ready. Wait a few seconds and re rerun
./launchJob.sh # Sets up the simple query benchmark
# Change the environmentId in `launchJob.sh` to "complex" and the connectionString in the first post request to "mysqltwo" in order to use the second database. Note that this has to match what is in `BenchmarkRUnner.java`
./launchJob.sh -c # Sets up the complex query benchmark
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

This demo is designed to showcase how a user could use the API key and username from the user service's response to connect to the public Kafka cluster to display processed results in their own app.

A connector and corresponding job needs to be running in order for the demo to work. For this you can run `setupFlink.sh` which will create a connector. After that, you can launch the job with `launchJob.sh`. If you run `launchJob.sh` without a `-c` flag, the default simple query will be used. If you want to change the name of the `environmentId` make sure that you also change it inside `server/index.ts`.

> If you want to test out the user and API key the user service responds with, you can copy the `environmentId` and `apiKey` from `launchJob.sh` response into `server/index.ts`.

#### Running the server (localhost:4000)

```bash
cd flow-demo-app
cd server
yarn
yarn ts-node index.ts # Edit apiKey, and environmentId if necessary, after starting the job.
# You will have to manually create file called environmentId.json with an empty array [] inside.
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

Creates a debezium connector for the given database. Introspects
the database and creates a topic in the kafka cluster with `environmentId.dbName` where dbName is inferred from the connection string. There is currently no check for whether the database is reachable or whether the user has root access to the database - this code needs to be commented back in in production.
This response returns a JWT that is set as an "Authorization" header to be used for the next request.

_Request_

```json
{
  "connectionString": "mysql://user:pass@mysql:3306/dbname",
  "environmentId": "environmentId"
}
```

_Response_

```json
{
  "data": "environmentId-connector"
}
```

Possible errors:

- ClientErrorJsonParseError (4000)
- ClientErrorMissingInput (4001)
- ClientErrorInvalidInput (4002)
- ServerErrorUnableToCreateDebeziumConnector (4003)
- ServerErrorBadConnection (4008)
- UnknownError (4999)

---

`/createQuery`

Verb: `POST`

Response Code: `200`

Launches a flink job with the given connection string, two sources, query sql, and sink sql. The result of the query will stream to the table name specified in the `sinkSql` argument. topic. The database name will be inferred from connection string.

> The column in the `sinkSql` argument needs to match the column created in the `querySql` argument.
> Both `sourceSql` and `sourceSqlTableTwo` arguments need to be specified for two different tables regardless of if there is a join in the query.
> Returns an access token that will be the user's password for the kafka ACL for the topic.

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
  "apiKey": "XXXX",
  "jobId": "XXXX"
}
```

Possible errors:

- ClientErrorJsonParseError (4000)
- ClientErrorMissingInput (4001)
- ClientErrorInvalidInput (4002)
- ServerErrorKafkaACLGeneration (4004)
- ClientErrorAuthorization (4005)
- ServerErrorUnableToCreateFlinkJob (4007)
- ServerErrorBadConnection (4008)
- UnknownError (4999)

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
kcat -b localhost:9093 -X security.protocol=SASL_PLAINTEXT -X sasl.mechanisms=SCRAM-SHA-256 -X sasl.username=simple -X sasl.password=1oHaupLjTiXqYCLs5MLGraQbLiQeLmWg2xFhH7cmung= -t simple.inventory.custom_output_table_name
```

<a name="deployment"/>

## Deployment

> Make sure the `KAFKA_USER` & `KAFKA_PASSWORD` variables are exported to the console.

1. Ssh into the remote gcp instance
2. Pull the latest code changes
   Git clone git@github.com:eemmiillyy/flowda.git
3. Build the JAR files for (flow-flink-job and flow-core)
   > Might need to clear the docker cach `docker system prune -a` since it takes around 3GB.
4. Change docker compose file: kafka advertised listener cannot be localhost (~L49) to IP of machine: `- KAFKA_CFG_ADVERTISED_LISTENERS=INTERNAL://kafka:9092,CLIENT://34.141.31.101:9093`

- Can also get IP of machine with `gcloud compute instances list` locally
- This means that kcat reading from topics will not work from inside the remote machine itself after this change

5. start the services

6. Run setupFlink.sh with STAGE=production
7. Edit Settings.json with new job id (Manual because this part of the script fails in production - WIP)
8. Re run setupFlink.sh so app is launched with new Settings.json
   > If you want to test with the remote database, you need to change all instances of the docker mysql database with the remote one (`mysql://root:[XXXX]@[IP of remote db]/inventory)
9. Run launchJob.sh
10. Test output topic is working with:

```bash
kcat -b [IP of remote machine]:[port] -X security.protocol=SASL_PLAINTEXT -X sasl.mechanisms=SCRAM-SHA-256 -X sasl.username=[username] -X sasl.password=[XXXX] -t [username].inventory.custom_output_table_name
```

11. Test output topic is working with demo app by changing line 30 in `server/index.ts` to IP of the remote
    machine instead of localhost.
12. Connect to the remote database with something like table plus and change a row in `products_on_hand` table to ensure that the output in the demo is updated.
