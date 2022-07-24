## Development

#### Start the services

```bash
docker-compose up -d
```

#### Start Flink (TODO - move into docker)

```bash
cd Downloads/flnk-1.15.0/
cd bin
./start-cluster.sh
```

**NOTE** If there are no resources for a job you need to manually start a task manager with `bin/taskmanager.sh start`
**NOTE** Flink config is inside `conf/flink-conf.yaml`

#### Build user service

```bash
cd flink-flow-job
mvn install
export SECRET=Chv1ocZ74xL9fl4hcJRfEvt6ZHmF6KlS1P6cDQBFdmjXOlwBJK0EAiYR1bdyzxVH STAGE=development && mvn -DskipTests=true clean package
```

**NOTE** We must manually upload the packaged .jar file from these steps to flink via the UI and retrieve it's job id. After that `Settings.json` jar file path needs to be updated.

```bash
cd flow-core
mvn install
export SECRET=[secret] STAGE=[stage] && mvn clean package && unset SECRET STAGE
```

**NOTE** stage can be set to `test` here if you would like to run the tests on build (default behavior). If you want to skip tests this value can be `development`.

#### Running the user service .jar (Ensure envs are set first)

```bash
java -jar target/flow.core-1.0-SNAPSHOT.jar
```

**NOTE** To kill java processes

```bash
ps -ef | grep java
kill -9 [PID]
```

#### Kafka manual config:

1. Open a bash shell in the docker container

```bash
docker exec -u root -it flowda_kafka_1 bash

```

Inside `/opt/bitnami/kafka/bin`

2. Create super user
   ```bash
   kafka-configs.sh --zookeeper zookeeper:2181 --alter --add-config 'SCRAM-SHA-256=[iterations=8192,password=bleepbloop],SCRAM-SHA-512=[password=bleepbloop]' --entity-type users --entity-name emily
   ```
3. give super user emily access to all topics
   ```bash
   kafka-acls.sh --authorizer-properties zookeeper.connect=zookeeper:2181 --add --allow-principal User:emily --operation ALL --topic "\*"
   ```
4. give super user emily access to all groups
   ```bash
   kafka-acls.sh --authorizer-properties zookeeper.connect=zookeeper:2181 --add --allow-principal User:emily --operation ALL --group *
   ```
5. Confirm on vm
   ```bash
   kafka-acls.sh --authorizer-properties zookeeper.connect=zookeeper:2181 --list
   ```
6. Confirm on host
   ```bash
   kcat -b localhost:9093 -X security.protocol=SASL_PLAINTEXT -X sasl.mechanisms=SCRAM-SHA-256 -X sasl.username=emily -X sasl.password="bleepbloop" -L
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

## Test

Tests use JUnit and Mockito. There are a combination of unit tests and integration tests. Each test suite for each domain should live beside the module they are testing.
`export SECRET=[secret] STAGE=test && mvn test && unset SECRET STAGE` to check the services are healthy

Run a single test
`mvn -Dtest=AppTest test`

Run a single test method
`mvn -Dtest=AppTest#methodname test`

## Running Benchmarks

`cd flow-benchmarks`
`java -jar target/benchmarks.jar`

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

CREATE TABLE orders (order_number BIGINT,purchaser BIGINT,quantity BIGINT,product_id BIGINT,event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND);

source:
CREATE TABLE real_table_name (quantity INT, product_id INT);"
add
event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND) WITH ('connector' = 'kafka','topic' = 'ENVIRONMENT.DBNAME.TABLENAME','properties.bootstrap.servers' = 'localhost:9093','properties.group.id' = '1391083','debezium-json.schema-include' = 'true', 'scan.startup.mode' = 'earliest-offset','format' = 'debezium-json')

aggregateSql
"SELECT SUM(quantity) as summed FROM real_table_name;"

sink
"CREATE TABLE custom_table_name (summed INT);"
add
WITH ('connector' = 'kafka','topic' = 'ENVIRONMENT.DATABASE.TABLENAME_OUTPUT','properties.bootstrap.servers' = 'localhost:9093','format' = 'debezium-json')

CREATE TABLE products_on_hand (quantity INT, product_id INT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND ) WITH ( 'connector' = 'kafka','topic' = 'tuesdayeight.inventory.products_on_hand', 'properties.bootstrap.servers' = 'localhost:9093', 'properties.group.id' = 'tuesdayeight', 'properties.sasl.mechanism' = 'SCRAM-SHA-256', 'properties.security.protocol' = 'SASL_PLAINTEXT', 'properties.sasl.jaas.config' = 'org.apache.kafka.common.security.scram.ScramLoginModule required username=emily password=bleepbloop;', 'debezium-json.schema-include' = 'true', 'scan.startup.mode' = 'earliest-offset', 'format' = 'debezium-json')

## Error Codes

`4000`: Unable to parse body of request. Needs to be JSON.
`4001`: Missing user input.
`4002`: User input validation error.
`4003`: Unable to communicate with debezium
`4004`: API key generation issue
`4005`: Kafka ACL rule creation issue
`4006`: Flink artefact generation issue. May be the result of faulty kafka connection.
`4007`: Issue running generated Flink job
`4008`: Unexpected/unhandled error during request
`4009`: Issue connecting to the database. Bad connection string or privileges.

## Debug

PLANETSCALE MONITORING
DATABASE_URL='mysql://byasxa4qr50u:pscale_pw_22gILJ5eVrzho1dlsFGACX2-rXtiXOx2-Ck7vgd8CBI@43cu7juzawsn.us-east-4.psdb.cloud/tester?sslaccept=strict'
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" localhost:8083/connectors/ -d '{ "name": "planetscale", "config": { "connector.class": "io.debezium.connector.mysql.MySqlConnector", "tasks.max": "1", "database.hostname": "43cu7juzawsn.us-east-4.psdb.cloud", "database.port": "3306", "database.user": "byasxa4qr50u", "database.password": "pscale_pw_22gILJ5eVrzho1dlsFGACX2-rXtiXOx2-Ck7vgd8CBI", "database.server.id": "184056", "database.server.name": "planetscale", "database.include.list": "tester", "database.history.kafka.bootstrap.servers": "kafka:9092", "database.history.kafka.topic": "dbhistory.planetscale", "database.allowPublicKeyRetrieval":"true", "database.ssl.mode": "preferred", "snapshot.locking.mode": "none" } }'

DEBEZIUM CONNECTOR (to monitor mysql binlog, tutorial configured for one topic and one replica)
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" localhost:8083/connectors/ -d '{ "name": "inventory-connector", "config": { "connector.class": "io.debezium.connector.mysql.MySqlConnector", "tasks.max": "1", "database.hostname": "mysql", "database.port": "3306", "database.user": "root", "database.password": "debezium", "database.server.id": "22", "database.server.name": "inventory", "database.include.list": "inventory", "database.history.kafka.bootstrap.servers": "kafka:9092", "database.history.kafka.topic": "dbhistory.inventory" } }'

VEIRFY DEBEZIUM CONNECTOR
curl -H "Accept:application/json" localhost:8083/connectors/

REVIEW THE DEBEZIUM CONNECTOR TASK (1 task per connector)
curl -i -X GET -H "Accept:application/json" localhost:8083/connectors/inventory-connector

WATCH TOPIC FOR CUSTOMERS TABLE
docker run -it --rm --name watcher --link zookeeper:zookeeper --link kafka:kafka quay.io/debezium/kafka:1.9 watch-topic -a -k dbserver1.inventory.customers

CHECK TOPICS INSIDE KAFKA CONTAINER CLI
bin/kafka-topics.sh --bootstrap-server=kafka:9092 --list

TRY AND ACCESS TOPIC LOCALLY
kcat -b localhost:9093 -t dbserver1.inventory.customers

PUBLISH TO TEST TOPIC ON MACHINE

kcat -b localhost:9093 -X security.protocol=SASL_PLAINTEXT -X sasl.mechanisms=SCRAM-SHA-256 -X sasl.username=emily -X sasl.password=bleepbloop -L

CREATE TEST TOPIC IN DOCKER

kafka-topics.sh --create --bootstrap-server kafka:9092 --topic newtopicbanned --replication-factor 1 --partitions 1

READ TOPIC FROM DEFAULT USER

kcat -b localhost:9093 -X security.protocol=SASL_PLAINTEXT -X sasl.mechanisms=SCRAM-SHA-256 -X sasl.username=emily -X sasl.password=bleepbloop= -L

{"programArgsList" : [

"--source","CREATE TABLE products_on_hand (quantity INT, product_id INT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)WITH ('debezium-json.schema-include'='true','scan.startup.mode'='earliest-offset','connector'='kafka','topic'='emily.inventory.products_on_hand','properties.bootstrap.servers'='localhost:9093','properties.group.id'='emily','properties.sasl.mechanism'='SCRAM-SHA-256','properties.security.protocol'='SASL_PLAINTEXT','properties.sasl.jaas.config'='org.apache.kafka.common.security.scram.ScramLoginModule required username=emily password=bleepbloop;','format'='debezium-json')",

"--sourceTwo","CREATE TABLE orders (order_number BIGINT, purchaser BIGINT, quantity BIGINT, product_id BIGINT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)WITH ('debezium-json.schema-include'='true','scan.startup.mode'='earliest-offset','connector'='kafka','topic'='emily.inventory.orders','properties.bootstrap.servers'='localhost:9093','properties.group.id'='emily','properties.sasl.mechanism'='SCRAM-SHA-256','properties.security.protocol'='SASL_PLAINTEXT','properties.sasl.jaas.config'='org.apache.kafka.common.security.scram.ScramLoginModule required username=emily password=bleepbloop;','format'='debezium-json')",

"--query", "SELECT SUM(quantity) as summed FROM products_on_hand",

"--sink","CREATE TABLE custom_output_table_name (summed INT)WITH ('connector'='kafka','topic'='emily.inventory.custom_output_table_name','properties.bootstrap.servers'='localhost:9093','properties.group.id'='emily','properties.sasl.mechanism'='SCRAM-SHA-256','properties.security.protocol'='SASL_PLAINTEXT','properties.sasl.jaas.config'='org.apache.kafka.common.security.scram.ScramLoginModule required username=emily password=bleepbloop;','format'='debezium-json')",

"--table","custom_output_table_name"],"parallelism": 1}
