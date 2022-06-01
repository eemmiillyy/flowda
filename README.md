## Development

`mvn install`

`java [FILENAME]`

## Build

`mvn package` generates in target/[FILENAME].jar
`java -cp target/pdpDataProjections-1.0-SNAPSHOT.jar pdpDataProjections.App`

## General

https://debezium.io/documentation/reference/stable/tutorial.html#considerations-running-debezium-docker

ZOOKEEPER
docker run -it --rm --name zookeeper -p 2181:2181 -p 2888:2888 -p 3888:3888 quay.io/debezium/zookeeper:1.9

KAFKA
docker run -it --rm --name kafka -p 9092:9092 --link zookeeper:zookeeper quay.io/debezium/kafka:1.9

MYSQL DB
docker run -it --rm --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=debezium -e MYSQL_USER=mysqluser -e MYSQL_PASSWORD=mysqlpw quay.io/debezium/pdpDataProjections-mysql:1.9

MYSQL CLIENT
docker run -it --rm --name mysqlterm --link mysql --rm mysql:8.0 sh -c 'exec mysql -h"$MYSQL_PORT_3306_TCP_ADDR" -P"$MYSQL_PORT_3306_TCP_PORT" -uroot -p"$MYSQL_ENV_MYSQL_ROOT_PASSWORD"'

KAFKA CONNECT (DEBEZIUM, has REST API to manage Debezium connectors)
docker run -it --rm --name connect -p 8083:8083 -e GROUP_ID=1 -e CONFIG_STORAGE_TOPIC=my_connect_configs -e OFFSET_STORAGE_TOPIC=my_connect_offsets -e STATUS_STORAGE_TOPIC=my_connect_statuses --link zookeeper:zookeeper --link kafka:kafka --link mysql:mysql quay.io/debezium/connect:1.9

KAFKA CONNECT VERIFY
curl -H "Accept:application/json" localhost:8083/
{"version":"3.1.0","commit":"cb8625948210849f"}
curl -H "Accept:application/json" localhost:8083/connectors/

DEBEZIUM CONNECTOR (to monitor mysql binlog, tutorial configured for one topic and one replica)
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" localhost:8083/connectors/ -d '{ "name": "inventory-connector", "config": { "connector.class": "io.debezium.connector.mysql.MySqlConnector", "tasks.max": "1", "database.hostname": "mysql", "database.port": "3306", "database.user": "debezium", "database.password": "dbz", "database.server.id": "184054", "database.server.name": "dbserver1", "database.include.list": "inventory", "database.history.kafka.bootstrap.servers": "kafka:9092", "database.history.kafka.topic": "dbhistory.inventory" } }'

VEIRFY DEBEZIUM CONNECTOR
curl -H "Accept:application/json" localhost:8083/connectors/

REVIEW THE DEBEZIUM CONNECTOR TASK (1 task per connector)
curl -i -X GET -H "Accept:application/json" localhost:8083/connectors/inventory-connector

WATCH TOPIC FOR CUSTOMERS TABLE
docker run -it --rm --name watcher --link zookeeper:zookeeper --link kafka:kafka quay.io/debezium/kafka:1.9 watch-topic -a -k dbserver1.inventory.customers

CHANGE DATA
UPDATE customers SET first_name='Anne Marie' WHERE id=1004;

VERIFY
mysql> use inventory;
mysql> show tables;
mysql> SELECT \* FROM customers;

STOP EVERYTHING
docker stop mysqlterm watcher connect mysql kafka zookeeper

FLINK
Installed at /Applications/flink-1.15.0
./bin/start-cluster.sh
localhost:8081 to access the Flink UI Dashboard.

FLINK JOB
./bin/flink run pdpDataProjectionss/streaming/WordCount.jar

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
