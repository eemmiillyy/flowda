`/createConnection`
Verb: POST
Response Code: 200
Creates a debezium/kafka connector for the given database. Introspects
the database and creates a topic in the kafka cluster with environmentId.dbName...
Request
{
"connectionString": "mysql://user:pass@mysql:3306/dbname",
"environmentId": "uniqueIdToUSeAsKafkaTopic"
}
Response
{
....
}

`/createQuery`
Verb: POST
Response Code: 200
Creates a Flink job that runs an aggregate sum query on the given
table name and given column. Publishes the output to the same
kafka cluster under the environmentId.dbName.tableName.fieldName_output topic.
Requires a JWT cookie.
Returns an access token that will be the users password for the kafka ACL for
the topic.
{
"connectionString": "mysql://user:pass@mysql:3306/dbname", // Will be removed
"environmentId": "uniqueIdToUSeAsKafkaTopic", // Will be removed
"databaseName": "dbname",
"tableName": "tableName",
"fieldName": "fieldName"
}
Response
{
"name": "successfully started Flink job.",
"environmentId" : "XXXX",
"ApiKey": "XXXX",
"jobId: "XXXX"
}

`/checkJobStatus`
Verb: GET
Response Code: 200
Returns the status of the job id passed in via the body.
Request
{ "jobId": "XXXXX" }
Response
{
"name": "..."
}

server.properties
consumer.properties
connect.properties

PUBLISH TO TEST TOPIC ON MACHINE

kcat -b localhost:9093 -X security.protocol=SASL_PLAINTEXT -X sasl.mechanisms=PLAIN -X sasl.username=user -X sasl.password=bitnami -t newtop -P test

CREATE TEST TOPIC IN DOCKER

kafka-topics.sh --create --bootstrap-server kafka:9092 --topic newtopicbanned --replication-factor 1 --partitions 1

READ TOPIC FROM DEFAULT USER

kcat -b localhost:9093 -X security.protocol=SASL_PLAINTEXT -X sasl.mechanisms=PLAIN -X sasl.username=emily -X sasl.password=bleepbloop -L

DYNAMICALLY ADD MASTER SCRAM USER (for createQuery to work)
cd opt/bitnami/kafka/bin/
kafka-configs.sh --zookeeper zookeeper:2181 --alter --add-config 'SCRAM-SHA-256=[iterations=8192,password=bleepbloop],SCRAM-SHA-512=[password=bleepbloop]' --entity-type users --entity-name emily

kafka-configs.sh --zookeeper zookeeper:2181 --alter --add-config 'SCRAM-SHA-256=[iterations=8192,password=test],SCRAM-SHA-512=[password=test]' --entity-type users --entity-name test

kafka-configs.sh --zookeeper zookeeper:2181 --alter --add-config 'SCRAM-SHA-256=[iterations=8192,password=alice-secret],SCRAM-SHA-512=[password=alice-secret]' --entity-type users --entity-name alice

kafka-configs.sh --zookeeper zookeeper:2181 --alter --add-config 'SCRAM-SHA-256=[iterations=8192,password=bleepbloop],SCRAM-SHA-512=[password=bleepbloop]' --entity-type users --entity-name emilyoop2

ADD ACL FOR NEW USER

kafka-acls.sh --authorizer-properties zookeeper.connect=zookeeper:2181 --add --allow-principal User:emily --operation ALL --topic "\*"

kafka-acls.sh --authorizer-properties zookeeper.connect=zookeeper:2181 --add --allow-principal User:alice --operation ALL --topic "eemmiillyy." --resource-pattern-type PREFIXED

kafka-acls.sh --authorizer-properties zookeeper.connect=zookeeper:2181 --add --allow-principal User:emilyoop2 --operation ALL --topic \*

THEN

kcat -b localhost:9093 -X security.protocol=SASL_PLAINTEXT -X sasl.mechanisms=SCRAM-SHA-256 -X sasl.username=alice -X sasl.password=alice-secret -L

kcat -b localhost:9093 -X security.protocol=SASL_PLAINTEXT -X sasl.mechanisms=SCRAM-SHA-256 -X sasl.username=emily -X sasl.password=bleepbloop -L

kcat -b localhost:9093 -X security.protocol=SASL_PLAINTEXT -X sasl.mechanisms=SCRAM-SHA-256 -X sasl.username=test -X sasl.password=test -L

LIST ACLS

kafka-acls.sh --authorizer-properties zookeeper.connect=zookeeper:2181 --list

IN SERVER PROPERTIES

authorizer.class.name=kafka.security.authorizer.AclAuthorizer
super.users=User:emily;User:ANONYMOUS

deploy
install vim
deploy connector
edit server.properties
add super user emily with password
give super user emily access to all topics
give super user emily access to all groups
restart

Working commands to secure a topic:

docker exec -u root pdpdataprojections_kafka_1 bash -c "cd opt/bitnami/kafka/bin && kafka-configs.sh --zookeeper zookeeper:2181 --alter --add-config 'SCRAM-SHA-256=[iterations=8192,password=tester],SCRAM-SHA-512=[password=tester]' --entity-type users --entity-name environmentId"

docker exec -u root pdpdataprojections_kafka_1 bash -c "cd opt/bitnami/kafka/bin && kafka-configs.sh --zookeeper zookeeper:2181 --alter --add-config 'SCRAM-SHA-256=[iterations=8192,password=bleepbloop],SCRAM-SHA-512=[password=bleepbloop]' --entity-type users --entity-name emily"

docker exec -u root pdpdataprojections_kafka_1 bash -c "cd opt/bitnami/kafka/bin && kafka-acls.sh --authorizer-properties zookeeper.connect=zookeeper:2181 --add --allow-principal User:environmentId --operation ALL --topic "environmentId." --resource-pattern-type PREFIXED"

kcat -b localhost:9093 -X security.protocol=SASL_PLAINTEXT -X sasl.mechanisms=SCRAM-SHA-256 -X sasl.username=fridaysix -X sasl.password="X32+lVL+Vdjz+UCa09F9UkDVpguCmkRSAf3qw3Xcm94=" -L

VIEW GROUP ACLS IN ZOOKEEPER
kafka-consumer-groups.sh --list --bootstrap-server zookeeper:2181

kafka-acls.sh --authorizer-properties zookeeper.connect=zookeeper:2181 --add --allow-principal User:emilyoop --operation ALL --group \*

kafka-acls.sh --authorizer-properties zookeeper.connect=zookeeper:2181 --add --allow-principal User:emily --operation ALL --topic '\*'

kafka-acls.sh --authorizer-properties zookeeper.connect=zookeeper:2181 --add --allow-principal User:emilyoop2 --operation ALL --topic '\*'

kcat -b localhost:9093 -X security.protocol=SASL_PLAINTEXT -X sasl.mechanisms=SCRAM-SHA-256 -X sasl.username=user -X sasl.password="bitnami" -L
