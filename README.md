## Development

`mvn install`

`docker-compose up -d` to start the services

`java [FILENAME]` (Only works with no dependencies. If dependencies then use the IDE play button)

## Test

`mvn test` to check the services are healthy

Run a single test
`mvn -Dtest=AppTest test`

Run a single test method
`mvn -Dtest=AppTest#methodname test`

## Build

`mvn package` generates in target/[FILENAME].jar
`java -cp target/pdpDataProjections-1.0-SNAPSHOT.jar pdpDataProjections.App`

## New flow

`docker-compose up -d`
configure connector, configure topic (db or tables to listen to)
test with remote database

## General

https://debezium.io/documentation/reference/stable/tutorial.html#considerations-running-debezium-docker

ZOOKEEPER
docker run -it --rm --name zookeeper -p 2181:2181 -p 2888:2888 -p 3888:3888 quay.io/debezium/zookeeper:1.9

KAFKA
docker run -it --rm --name kafka -p 9092:9092 --link zookeeper:zookeeper quay.io/debezium/kafka:1.9

MYSQL DB
docker run -it --rm --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=debezium -e MYSQL_USER=mysqluser -e MYSQL_PASSWORD=mysqlpw quay.io/debezium/example-mysql:1.9

MYSQL CLIENT
docker run -it --rm --name mysqlterm --link mysql --rm mysql:8.0 sh -c 'exec mysql -h"$MYSQL_PORT_3306_TCP_ADDR" -P"$MYSQL_PORT_3306_TCP_PORT" -uroot -p"$MYSQL_ENV_MYSQL_ROOT_PASSWORD"'

KAFKA CONNECT (DEBEZIUM, has REST API to manage Debezium connectors)
docker run -it --rm --name connect -p 8083:8083 -e GROUP_ID=1 -e CONFIG_STORAGE_TOPIC=my_connect_configs -e OFFSET_STORAGE_TOPIC=my_connect_offsets -e STATUS_STORAGE_TOPIC=my_connect_statuses --link zookeeper:zookeeper --link kafka:kafka --link mysql:mysql quay.io/debezium/connect:1.9

KAFKA CONNECT VERIFY
curl -H "Accept:application/json" localhost:8083/
{"version":"3.1.0","commit":"cb8625948210849f"}
curl -H "Accept:application/json" localhost:8083/connectors/

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
LONG RUNNING PROCESS WATCHING EVENTS ON SPECIFIC TOPIC FROM KAFKA CONTAINER CLI
bin/kafka-console-consumer.sh --topic dbserver1.inventory.customers --from-beginning --bootstrap-server kafka:9092

TRY AND ACCESS TOPIC LOCALLY
https://stackoverflow.com/questions/64283594/kafka-events-published-from-the-host-machine-are-not-consumed-by-the-applicatio EXPLANATION
kcat -b localhost:9093 -t dbserver1.inventory.customers

CHANGE DATA
UPDATE customers SET first_name='Anne Marie' WHERE id=1004;

VERIFY
mysql> use inventory;
mysql> show tables;
mysql> SELECT \* FROM customers;

STOP EVERYTHING
docker stop mysqlterm watcher connect mysql kafka zookeeper

FLINK
Installed at /Users/emilymorgan/Downloads/flink-1.15.0
./bin/start-cluster.sh
localhost:8081 to access the Flink UI Dashboard.

FLINK JOB
./bin/flink run pdpDataProjections/streaming/WordCount.jar

WITH ARGUMENTS
/bin/flink run /Users/emilymorgan/Desktop/pdpDataProjections/target/pdpDataProjections-1.0-SNAPSHOT.jar --topicName test --schemaContents test2 --sqlQuery test3

STOP FLINK
./bin/stop-cluster.sh

LIMITATIONS

- Postgres on Heroku may not be possible - may require restarting the server to pick up the new log settings which may not be possible.

- Flink's execution environment can be local, with YARN, or with TEZ, or MESOS.
  // CORE HERE
- Flink's API: need to use their DataStream API rather than DataSet API. Can go through Kafka again for the stream. Supports only Java and Scala right now. Table API supports SQL queries which are converted internally to DataStream APIs so you don't have to know Java or Scala.
- DataStream API is for real time NOT batch.

Steps:

1. Create a .JAR file to FLINK.
2. Submit the job to the execution environment (maybe a cluster) using a client
3. Client trigger the job manager takes care of resource management and scheduling
4. Task manager is triggered on each instance in a cluster for pdpDataProjections.
5. Monitor job using the dashboard interface (Running, completed, task managers)

Apache Flink maintains the state of your stream using independent events to maintain aggregations.

SWITCHING JAVA VERSION
https://stackoverflow.com/questions/21964709/how-to-set-or-change-the-default-java-jdk-version-on-macos

Binary is in scala, the source has the actual flink source code.
