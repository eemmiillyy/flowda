# API Documentation

## `POST /create-database-connection/`

Sets up Debezium connection via POST request to service. Kafka must be up and running already.

connectionString: string - connection to your database. Must have root access.
environmentId: string - environment id from PDP.

    curl -i -H 'Accept: application/json' -d 'connectionString=XXXXX&environmentId=XXXXX' http://localhost:7000/create-database-connection

### Response

    HTTP/1.1 201 Created
    Date: Thu, 24 Feb 2011 12:36:30 GMT
    Status: 201 Created
    Connection: close
    Content-Type: application/json
    Location: /thing/1
    Content-Length: 36

    {"id":1,"name":"Foo","status":"new"}

## `POST /create-cached-query/`

Creates a kafka connector on Flink by publishing the given arguments in a JSON blob to socket 7001.
Flink listens on socket 7001 and when it receives a message it creates a connector and query.

connectionString: string - connection to your database. Must have root access.
environmentId: string - environment id from PDP.
schema: string - prisma schema of your connected database.
sqlQuery: string - aggregate sql query you want to run.

    curl -i -H 'Accept: application/json' -d 'connectionString=AAAAA&environmentId=XXXXX&schema=YYYYY&sqlQuery=ZZZZZ' http://localhost:7000/create-cached-query

### Response

    HTTP/1.1 201 Created
    Date: Thu, 24 Feb 2011 12:36:30 GMT
    Status: 201 Created
    Connection: close
    Content-Type: application/json
    Location: /thing/1
    Content-Length: 36

    {"id":1,"name":"Foo","status":"new"}
