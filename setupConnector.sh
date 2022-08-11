
# Create connector
OUTPUT=$(curl POST -i -v http://localhost:8888/createConnection -H "Content-Type: application/json" -d '{
    "connectionString": "mysql://debezium:dbz@mysql:3306/inventory",
    "environmentId": "simple"
}')

# Authorization:Bearer xxxx
TOKEN="$(echo "${OUTPUT//[[:space:]]/}"  tr -d '[:blank:]' | sed -e 's/{"data".*//' | sed -e 's/content.*//' | sed -e 's/.*200OK//' | sed -e 's/Bearer/& /g')"
echo $TOKEN
BEARER='Authorization'
if [[ "$TOKEN" != *"$BEARER"* ]]; then
  echo "response header did not contain jwt. ABORTING!"
  exit 1
fi

# Choose query type, -c for complex. Defaults to simple
while getopts cs: name
do case $name in 
  c)   cFlag=1;;
  ?)  printf "Specify c for complex query, otherwise default simple query will be used." exit 2;;
  esac
done

# Run query
if [ ! -z "$cFlag" ]; then
  printf 'Running complex query...'
  RESP=$(curl -X POST -v http://localhost:8888/createQuery -H "$TOKEN" -H "Content-Type: application/json"  \
-d '{
  "connectionString": "mysql://debezium:dbz@mysqltwo:3306/inventory",
  "sourceSql": "CREATE TABLE customers (id INT, first_name VARCHAR, last_name VARCHAR, email VARCHAR, event_time TIMESTAMP(3) METADATA FROM '\''value.source.timestamp'\'' VIRTUAL, WATERMARK FOR event_time AS event_time - INTERVAL '\''5'\'' SECOND)",
  "sourceSqlTableTwo": "CREATE TABLE orders (order_number BIGINT, purchaser BIGINT, quantity BIGINT, product_id BIGINT, event_time TIMESTAMP(3) METADATA FROM '\''value.source.timestamp'\'' VIRTUAL,  WATERMARK FOR event_time AS event_time - INTERVAL '\''5'\'' SECOND)",
  "querySql": "SELECT customers.email, COUNT(orders.order_number) AS orderCount, SUM(orders.quantity) AS orderQuantity FROM customers INNER JOIN orders ON customers.id = orders.purchaser GROUP BY customers.email ORDER BY orderQuantity DESC LIMIT 1",
  "sinkSql": "CREATE TABLE custom_output_table_name (email VARCHAR, orderCount BIGINT, orderQuantity BIGINT)"
}')
echo $RESP
exit 0
fi  

printf 'Running default simple query...'
  RESP=$(curl -X POST -v http://localhost:8888/createQuery -H "$TOKEN" -H "Content-Type: application/json"  \
-d '{
  "connectionString": "mysql://debezium:dbz@mysql:3306/inventory",
  "sourceSql": "CREATE TABLE products_on_hand (quantity INT, product_id INT, event_time TIMESTAMP(3) METADATA FROM '\''value.source.timestamp'\'' VIRTUAL, WATERMARK FOR event_time AS event_time - INTERVAL '\''5'\'' SECOND)",
  "sourceSqlTableTwo": "CREATE TABLE orders (order_number BIGINT, purchaser BIGINT, quantity BIGINT, product_id BIGINT, event_time TIMESTAMP(3) METADATA FROM '\''value.source.timestamp'\'' VIRTUAL,  WATERMARK FOR event_time AS event_time - INTERVAL '\''5'\'' SECOND)",
  "querySql": "SELECT SUM(quantity) as summed FROM products_on_hand",
  "sinkSql": "CREATE TABLE custom_output_table_name (summed INT)"
}')
echo $RESP
