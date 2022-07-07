## Development

`docker-compose up -d` to start the services

cd `Downloads/flnk-1.15.0/`
// to add a task manager
// if there are no resources for a job you need to manually start a task manager
`bin/taskmanager.sh start`
to edit flink app configuration
`conf/flink-conf.yaml`

`cd processingSource`
`mvn install`
`mvn package`

`cd userSource`
`mvn install`
`mvn package`
`java -jar target/userSource-1.0-SNAPSHOT.jar`
Kill java PID (kill -9 [PID]) with `ps -ef | grep java`

### Kafka manual config:

1. install vim (login with root, then `apt-get update` and `apt-get install vim`)
2. edit `/opt/bitnami/kafka/config/server.properties`

```
authorizer.class.name=kafka.security.authorizer.AclAuthorizer
super.users=User:emily;User:ANONYMOUS
```

3. Create super user
   ```bash
   kafka-configs.sh --zookeeper zookeeper:2181 --alter --add-config 'SCRAM-SHA-256=[iterations=8192,password=bleepbloop],SCRAM-SHA-512=[password=bleepbloop]' --entity-type users --entity-name emily
   ```
4. give super user emily access to all topics
   ```bash
   kafka-acls.sh --authorizer-properties zookeeper.connect=zookeeper:2181 --add --allow-principal User:emily --operation ALL --topic "\*"
   ```
5. give super user emily access to all groups
   ```bash
   kafka-acls.sh --authorizer-properties zookeeper.connect=zookeeper:2181 --add --allow-principal User:emilyoop --operation ALL --group \*
   ```
6. restart
7. Confirm on vm
   ```bash
   kafka-acls.sh --authorizer-properties zookeeper.connect=zookeeper:2181 --list
   ```
8. Confirm on host
   ```bash
   kcat -b localhost:9093 -X security.protocol=SASL_PLAINTEXT -X sasl.mechanisms=SCRAM-SHA-256 -X sasl.username=user -X sasl.password="bleeopbloop" -L
   ```

## Re-encrypting a phrase

1. Change the field (prefixed with $$) to the plaintext version
2. Edit `Server.java` to:

```java
    Settings settings = new Settings("development");
    settings.encrypt();
    settings.decrypt();
```

3. Replace `Settings.json` values with encrypted ones.
   **NOTE** right now all fields prefixed with `$$` inside of each specified stage will be
   encrypted and decrypted with the command from step 2.
   If you want to encrypt or decrypt only a single field at a time you can use `encryptField(String field)` or `decryptField(String field)`

## Test

`mvn test` to check the services are healthy

Run a single test
`mvn -Dtest=AppTest test`

Run a single test method
`mvn -Dtest=AppTest#methodname test`

## API Reference

`/createConnection`

Verb: `POST`

Response Code: `200`

Creates a debezium/kafka connector for the given database. Introspects
the database and creates a topic in the kafka cluster with environmentId.dbName...

Request

```json
{
  "connectionString": "mysql://user:pass@mysql:3306/dbname",
  "environmentId": "uniqueIdToUSeAsKafkaTopic"
}
```

Response

```json
{}
```

---

`/createQuery`

Verb: `POST`

Response Code: `200`

Creates a Flink job that runs an aggregate sum query on the given
table name and given column. Publishes the output to the same
kafka cluster under the environmentId.dbName.tableName.fieldName_output topic.
Requires a JWT cookie.
Returns an access token that will be the users password for the kafka ACL for
the topic.

Request

```json
{
  "connectionString": "mysql://user:pass@mysql:3306/dbname", // Will be removed
  "environmentId": "uniqueIdToUSeAsKafkaTopic", // Will be removed
  "databaseName": "dbname",
  "tableName": "tableName",
  "fieldName": "fieldName"
}
```

Response

```json
{
  "name": "successfully started Flink job.",
  "environmentId": "XXXX",
  "ApiKey": "XXXX",
  "jobId": "XXXX"
}
```

---

`/checkJobStatus`

Verb: `GET`

Response Code: `200`

Returns the status of the job id passed in via the body.

Request

```json
{ "jobId": "XXXXX" }
```

Response

```json
{
  "name": "..."
}
```

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

kcat -b localhost:9093 -X security.protocol=SASL_PLAINTEXT -X sasl.mechanisms=PLAIN -X sasl.username=emily -X sasl.password=bleepbloop -L
