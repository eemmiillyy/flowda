# # Create connector
OUTPUT=$(curl POST -i -v http://localhost:8888/createConnection -H "Content-Type: application/json" -d '{
    "connectionString": "mysql://debezium:dbz@mysql:3306/inventory",
    "environmentId": "tester"
}')

TOKEN="$(echo "${OUTPUT//[[:space:]]/}"  tr -d '[:blank:]' | sed -e 's/{"data".*//' | sed -e 's/content.*//' | sed -e 's/.*200OK//' | sed -e 's/Bearer/& /g')"

echo $TOKEN
BEARER='Authorization'
if [[ "$TOKEN" != *"$BEARER"* ]]; then
  echo "response header did not contain jwt. ABORTING!"
  exit 1
fi

RESP=$(curl -X POST -v http://localhost:8888/createQuery -H "$TOKEN" -H "Content-Type: application/json"  \
-d '{
    "connectionString": "mysql://debezium:dbz@mysql:3306/inventory",
 "sourceSql": "CREATE TABLE products_on_hand (quantity INT, product_id INT, event_time TIMESTAMP(3) METADATA FROM '\''value.source.timestamp'\'' VIRTUAL, WATERMARK FOR event_time AS event_time - INTERVAL '\''5'\'' SECOND)",
  "sourceSqlTableTwo": "CREATE TABLE orders (order_number BIGINT, purchaser BIGINT, quantity BIGINT, product_id BIGINT, event_time TIMESTAMP(3) METADATA FROM '\''value.source.timestamp'\'' VIRTUAL,  WATERMARK FOR event_time AS event_time - INTERVAL '\''5'\'' SECOND)",
  "querySql": "SELECT SUM(quantity) as summed FROM products_on_hand",
  "sinkSql": "CREATE TABLE custom_output_table_name (summed INT)"
}')
echo $RESP
