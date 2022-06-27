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

DYNAMICALLY ADD SCRAM USER

kafka-configs.sh --zookeeper zookeeper:2181 --alter --add-config 'SCRAM-SHA-256=[iterations=8192,password=bleepbloop],SCRAM-SHA-512=[password=bleepbloop]' --entity-type users --entity-name emily

kafka-configs.sh --zookeeper zookeeper:2181 --alter --add-config 'SCRAM-SHA-256=[iterations=8192,password=alice-secret],SCRAM-SHA-512=[password=alice-secret]' --entity-type users --entity-name alice

THEN

kcat -b localhost:9093 -X security.protocol=SASL_PLAINTEXT -X sasl.mechanisms=SCRAM-SHA-256 -X sasl.username=alice -X sasl.password=alice-secret -L

kcat -b localhost:9093 -X security.protocol=SASL_PLAINTEXT -X sasl.mechanisms=PLAIN -X sasl.username=emily -X sasl.password=bleepbloop -L

ADD ACL FOR NEW USER

kafka-acls.sh --authorizer-properties zookeeper.connect=zookeeper:2181 --add --allow-principal User:emily --operation ALL --topic "\*"

kafka-acls.sh --authorizer-properties zookeeper.connect=zookeeper:2181 --add --allow-principal User:alice --operation ALL --topic "newtopicbanned"

bin/kafka-acls.sh --add --cluster --operation Create --authorizer-properties zookeeper.connect=zookeeper:2181 --allow-principal User:admin

IN SERVER PROPERTIES

authorizer.class.name=kafka.security.authorizer.AclAuthorizer
super.users=User:emily;User:ANONYMOUS

deploy
edit server.properties
restart
create topic
test read (blocked)
create scram user 1
create scram user 2
test reads work for both now
add ACL for user 1 on all topics
user 2 is blocked now
create topic 2
check user 1 can still read
add ACL for user 2 for this topic
check user 2 can read
works!
