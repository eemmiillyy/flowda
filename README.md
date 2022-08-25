## Development

#### Start the services

```bash
docker-compose up -d
```

#### Build the job

```bash
cd flow-flink-job
mvn install
mvn -DskipTests=true clean package
```

#### Build the server

```bash
cd flow-core
export SECRET=[secret] STAGE=test
mvn install
mvn -DskipTests=true clean package
```

#### Upload the job and start server

```bash
export STAGE=development
`./setupFlink.sh`
```

## API Reference

`/createConnection`

Verb: `POST`

Response Code: `200`

Creates a debezium/kafka connector for the given database. Introspects
the database and creates a topic in the kafka cluster with `environmentId.dbName`. There is no check for whether the database is reachable. That needs to be manually ensured. There is also no check to the permissions. Needs to be manually run. Returns a JWT that is set as a cookie to be used for the next request.

_Request_

```json
{
  "connectionString": "mysql://user:pass@mysql:3306/dbname",
  "environmentId": "uniqueIdToUSeAsKafkaTopic"
}
```

_Response_

```json
{
  "data": "uniqueIdToUSeAsKafkaTopic-connector"
}
```

> A Json Web Token will be returned in the response header and needs to be used as the Authorization: Bearer <token> in the next request.

---

`/createQuery`

Verb: `POST`

Response Code: `200`

Launches a flink job with the given source, aggregate, and sink table. The result of the aggregate will stream to the environmentId.dbName.outputTableName topic. db name inferred from connection string. tablename will be inferred from source statement. agreggateSql tablename needs to match the source. output table name will be inferred from the sink statement. Returns an access token that will be the users password for the kafka ACL for
the topic. DO NOT add semi colons to end of the input

_Request_

```json
{
  "connectionString": "mysql://user:pass@mysql:3306/inventory",
  "sourceSql": "CREATE TABLE products_on_hand (quantity INT, product_id INT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)",
  "sourceSqlTableTwo": "CREATE TABLE orders (order_number BIGINT, purchaser BIGINT, quantity BIGINT, product_id BIGINT, event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL,  WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND)",
  "querySql": "SELECT SUM(quantity) as summed FROM products_on_hand",
  "sinkSql": "CREATE TABLE custom_output_table_name (summed INT)"
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

## Testing

Tests use JUnit and Mockito. There are a combination of unit tests and integration tests. Each test suite for each domain should live beside the module they are testing.

```bash
`cd flow-core`
`export SECRET=[secret] STAGE=test && mvn test`

`mvn -Dtest=[suiteName] test` # Run a single test
# E.g. `mvn -Dtest=CreateQueryEndpointTest test`

`mvn -Dtest=[suiteName]\#[methodName] test` # Run a single method in a test
# E.g. `mvn -Dtest=CreateQueryEndpointTest#testThrowsWithMissingConnectionStringQuery test`
```

## Debug

```bash
kcat -b localhost:9093 -X security.protocol=SASL_PLAINTEXT -X sasl.mechanisms=SCRAM-SHA-256 -X sasl.username=emily -X sasl.password=bleepbloop -t emilytwo.inventory.custom_output_table_name
```

```bash
kcat -b localhost:9093 -X security.protocol=SASL_SSL -X sasl.mechanisms=SCRAM-SHA-256 -X sasl.username=emily -X sasl.password=bleepbloop -X ssl.ca.location=/private/etc/ssl/flowda/ca-cert -L
```

## One way SSL between client and broker

https://docs.confluent.io/platform/current/security/security_tutorial.html#creating-ssl-keys-and-certificates

1. Generate keypair and self signed certificate inside `kafka.server.keystore.jks `(Creates one enty)
   `sudo keytool -keystore kafka.server.keystore.jks -alias localhost -keyalg RSA -validity {validity} -genkey`
2. Verified with `sudo keytool -list -v -keystore kafka.server.keystore.jks`.
3. Create custom certificate authority to sign the certificate (Needs to be a real CA in prod, generates ca-key and ca-cert).
   `sudo openssl req -new -x509 -keyout ca-key -out ca-cert -days {validity}`
4. The broker key/cert pair needs a corresponding trust store that contains the new CA authority that signed it's identificate certificate. Creates `kafka.server.truststore.jks`
   `keytool -keystore kafka.server.truststore.jks -alias CARoot -importcert -file ca-cert`
5. Verify with `keytool -list -v -keystore kafka.truststore.jks` should contain caroot entry.
6. Export the certificate before signing from step one with the custom authority. (Creates cert-file).
   `keytool -keystore kafka.server.keystore.jks -alias localhost -certreq -file cert-file`
7. Sign it (Creates cert-signed).
   `openssl x509 -req -CA ca-cert -CAkey ca-key -in cert-file -out cert-signed -days {validity} -CAcreateserial -passin pass:{ca-password}`
8. Import ca cert and signed cert into `kafka.server.keystore.jks`
   `keytool -keystore kafka.server.keystore.jks -alias CARoot -importcert -file ca-cert keytool -keystore kafka.server.keystore.jks -alias localhost -importcert -file cert-signed`
9. Place keystore and truststore files inside the `./keystore `and `./truststore` folders respectively.

## Consuming output

Clients consuming kafka topic output with their API key need to connect over SSL. In order to do that, they need to trust the certificate authority that signed the brokers certificate, or some authority up the chain that signed that certificate authority.
When this moves

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
SECRET=[secret] STAGE=development java -jar target/flow.core-1.0-SNAPSHOT.jar
```

**NOTE** Decryption will fail during this since it will try to decrypt the plaintext which will throw an error. That is expected.
**NOTE** If the stage passwords are different you need to

3. Replace `Settings.json` values with encrypted ones.
   **NOTE** right now all fields prefixed with `$$` inside of each specified stage will be
   encrypted and decrypted with the command from step 2.
   If you want to encrypt or decrypt only a single field at a time you can use `encryptField(String field)` or `decryptField(String field)`

4. Make sure it works with

```bash
SECRET=[secret] STAGE=development java -jar target/flow.core-1.0-SNAPSHOT.jar
```

Then undo changes to server and re run with new secret.

mysql://debezium:dbz@10.0.6.115:3306/inventory
mysql://debezium:dbz@10.0.6.115:3307/inventory
