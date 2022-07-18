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
cd processingSource
mvn install
mvn package
```

**NOTE** We must manually upload the packaged .jar file from these steps to flink via the UI and retrieve it's job id. After that `Settings.json` jar file path needs to be updated.

```bash
cd userSource
mvn install
export SECRET=[secret] STAGE=[stage] && mvn clean package && unset SECRET STAGE
```

**NOTE** stage can be set to `test` here if you would like to run the tests on build (default behavior). If you want to skip tests this value can be `development`.

#### Running the user service .jar

```bash
SECRET=[secret] STAGE=[stage] java -jar target/userSource-1.0-SNAPSHOT.jar
```

**NOTE** To kill java processes

```bash
ps -ef | grep java
kill -9 [PID]
```

#### Kafka manual config:

1. Open a bash shell in the docker container

```bash
docker exec -u root -it pdpdataprojections_kafka_1 bash
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
SECRET=[secret] STAGE=development java -jar target/userSource-1.0-SNAPSHOT.jar
```

**NOTE** Decryption will fail during this since it will try to decrypt the plaintext which will throw an error. That is expected.
**NOTE** If the stage passwords are different you need to

3. Replace `Settings.json` values with encrypted ones.
   **NOTE** right now all fields prefixed with `$$` inside of each specified stage will be
   encrypted and decrypted with the command from step 2.
   If you want to encrypt or decrypt only a single field at a time you can use `encryptField(String field)` or `decryptField(String field)`

4. Make sure it works with

```bash
SECRET=[secret] STAGE=development java -jar target/userSource-1.0-SNAPSHOT.jar
```

Then undo changes to server and re run with new secret.

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
the database and creates a topic in the kafka cluster with `environmentId.dbName`. There is no check for whether the database is reachable. That needs to be manually ensured. There is also no check to the permissions. Needs to be manually run.

_Request_

```json
{
  "connectionString": "mysql://user:pass@mysql:3306/dbname",
  "environmentId": "uniqueIdToUSeAsKafkaTopic"
}
```

_Response_

```json
{}
```

---

`/createQuery`

Verb: `POST`

Response Code: `200`

Creates a Flink job that runs an aggregate sum query on the given
table name and given column. Publishes the output to the same
kafka cluster under the environmentId.dbName.tableName.fieldName_output topic. Fieldname is not validated.
Requires a JWT cookie.
Returns an access token that will be the users password for the kafka ACL for
the topic.

_Request_

```json
{
  "connectionString": "mysql://user:pass@mysql:3306/dbname", // Will be removed
  "environmentId": "uniqueIdToUSeAsKafkaTopic", // Will be removed
  "databaseName": "dbname",
  "tableName": "tableName",
  "fieldName": "fieldName"
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

`4000`: Unable to parse body of request. Needs to be JSON.
`4001`: Missing user input.
`4002`: User input validation error.
`4003`: Unable to communicate with debezium
`4004`: API key generation issue
`4005`: Kafka ACL rule creation issue
`4006`: Flink artefact generation issue. May be the result of faulty kafka connection.
`4007`: Issue running generated Flink job
`4008`: Unexpected/unhandled error during request

## Debug

PLANETSCALE MONITORING
DATABASE_URL='mysql://byasxa4qr50u:pscale_pw_22gILJ5eVrzho1dlsFGACX2-rXtiXOx2-Ck7vgd8CBI@43cu7juzawsn.us-east-4.psdb.cloud/tester?sslaccept=strict'
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" localhost:8083/connectors/ -d '{ "name": "planetscale", "config": { "connector.class": "io.debezium.connector.mysql.MySqlConnector", "tasks.max": "1", "database.hostname": "43cu7juzawsn.us-east-4.psdb.cloud", "database.port": "3306", "database.user": "byasxa4qr50u", "database.password": "pscale_pw_22gILJ5eVrzho1dlsFGACX2-rXtiXOx2-Ck7vgd8CBI", "database.server.id": "184056", "database.server.name": "planetscale", "database.include.list": "tester", "database.history.kafka.bootstrap.servers": "kafka:9092", "database.history.kafka.topic": "dbhistory.planetscale", "database.allowPublicKeyRetrieval":"true", "database.ssl.mode": "preferred", "snapshot.locking.mode": "none" } }'

DEBEZIUM CONNECTOR (to monitor mysql binlog, tutorial configured for one topic and one replica)
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" localhost:8083/connectors/ -d '{ "name": "inventory-connector", "config": { "connector.class": "io.debezium.connector.mysql.MySqlConnector", "tasks.max": "1", "database.hostname": "mysql", "database.port": "3306", "database.user": "debezium", "database.password": "dbz", "database.server.id": "184054", "database.server.name": "dbserver1", "database.include.list": "inventory", "database.history.kafka.bootstrap.servers": "kafka:9092", "database.history.kafka.topic": "dbhistory.inventory" } }'

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

kcat -b localhost:9093 -X security.protocol=SASL_PLAINTEXT -X sasl.mechanisms=PLAIN -X sasl.username=user -X sasl.password=bitnami -t newtop -P test

CREATE TEST TOPIC IN DOCKER

kafka-topics.sh --create --bootstrap-server kafka:9092 --topic newtopicbanned --replication-factor 1 --partitions 1

READ TOPIC FROM DEFAULT USER

kcat -b localhost:9093 -X security.protocol=SASL_PLAINTEXT -X sasl.mechanisms=SCRAM-SHA-256 -X sasl.username=emily -X sasl.password=bleepbloop -L
