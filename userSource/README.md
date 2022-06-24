`/createConnection`
Verb: POST
Response Code: 200
Returns the status of the job id passed in via the body.
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
Returns the status of the job id passed in via the body.
Request
{
"connectionString": "mysql://user:pass@mysql:3306/dbname",
"environmentId": "uniqueIdToUSeAsKafkaTopic",
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
