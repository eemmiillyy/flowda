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
