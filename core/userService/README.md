# API Documentation

## `POST /createDatabaseConnection`

Sets up Debezium connection via POST request to service. Kafka must be up and running already.

connectionString: string - connection to your database. Must have root access.
environmentId: string - environment id from PDP to name the database as (topic in kafka aswell)

    curl -i -H 'Accept: application/json' -d 'connectionString=XXXXX&environmentId=XXXXX' http://localhost:7000/createDatabaseConnection

### Response

    HTTP/1.1 201 Created
    Date: Thu, 24 Feb 2011 12:36:30 GMT
    Status: 201 Created
    Connection: close
    Content-Type: application/json
    Location: /thing/1
    Content-Length: 36

    {"id":1,"name":"Foo","status":"new"}

## `POST /createCachedQuery`

Creates a kafka connector, and processor job on flink with the given input via REST API
Each flink service will connect and publish to the same kafka instance under a different topic (environment Id).

connectionString: string - connection to your database. Must have root access.
environmentId: string - environment id from PDP.
schema: string - prisma schema of your connected database.
sqlQuery: string - aggregate sql query you want to run.

    curl -i -H 'Accept: application/json' -d 'connectionString=AAAAA&environmentId=XXXXX&schema=YYYYY&sqlQuery=ZZZZZ' http://localhost:7000/createCachedQuery

### Response

    HTTP/1.1 201 Created
    Date: Thu, 24 Feb 2011 12:36:30 GMT
    Status: 201 Created
    Connection: close
    Content-Type: application/json
    Location: /thing/1
    Content-Length: 36

    {"id":1,"name":"Foo","status":"new"}

## `POST /deleteDatabaseConnection`

Deletes the kafka connector and cancels the flink job

connectionString: string - connection to your database. Must have root access.
environmentId: string - environment id from PDP.

    curl -i -H 'Accept: application/json' -d 'connectionString=AAAAA&environmentId=XXXXX' http://localhost:7000/deleteDatabaseConnection

### Response

    HTTP/1.1 201 Created
    Date: Thu, 24 Feb 2011 12:36:30 GMT
    Status: 201 Created
    Connection: close
    Content-Type: application/json
    Location: /thing/1
    Content-Length: 36

    {"id":1,"name":"Foo","status":"new"}

## `POST /checkJobStatus`

Checks the status of the kafka source connector and the flink job

connectionString: string - connection to your database. Must have root access.
environmentId: string - environment id from PDP.

    curl -i -H 'Accept: application/json' -d 'connectionString=AAAAA&environmentId=XXXXX' http://localhost:7000/checkJobStatus

### Response

    HTTP/1.1 201 Created
    Date: Thu, 24 Feb 2011 12:36:30 GMT
    Status: 201 Created
    Connection: close
    Content-Type: application/json
    Location: /thing/1
    Content-Length: 36

    {"id":1,"name":"Foo","status":"new"}
