package processingSource;

import org.apache.flink.api.java.utils.MultipleParameterTool;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableEnvironment;

public class DataFlowJob {

  public static void main(String[] args) {
    // Args
    final String SOURCE_KEY = "source";
    final String QUERY_KEY = "query";
    final String SINK_KEY = "sink";
    final String TABLE_KEY = "table";

    MultipleParameterTool params = MultipleParameterTool.fromArgs(args);
    CustomLogger customLogger = new CustomLogger();
    //if args is empty throw error
    if (
      !params.has(SOURCE_KEY) ||
      !params.has(QUERY_KEY) ||
      !params.has(SINK_KEY) ||
      !params.has(TABLE_KEY)
    ) {
      customLogger.error(
        "exception: need parameters for jobs, e.g. '[COMMAND] --source test --query test --sink test --tablename test'"
      );
    } else {
      //   System.out.println(params.get(SOURCE_KEY));
      //   System.out.println(params.get(QUERY_KEY));
      //   System.out.println(params.get(SINK_KEY));
      //   System.out.println(params.get(TABLE_KEY));
    }

    // TODO register tableEnv to a single catalog.
    /**
     * Catalog connect to external systems for reading and writing.
     * Tables are registered to the catalog.
     * Tables can be sources or sinks.
     */
    // Set up source table
    EnvironmentSettings env = EnvironmentSettings.newInstance().build();
    TableEnvironment tableEnv = TableEnvironment.create(env);

    // tableEnv.executeSql("CREATE TABLE orders (\n" +
    //     "    order_number BIGINT,\n" +
    //     "    purchaser BIGINT,\n" +
    //     "    quantity BIGINT,\n" +
    //     "    product_id BIGINT,\n" +
    //     "    event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, \n" +
    //     "    WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND \n" +
    //     ") WITH (\n" +
    //     "    'connector' = 'kafka',\n" +
    //     "    'topic'     = 'dbserver1.inventory.orders',\n" +
    //     "    'properties.bootstrap.servers' = 'localhost:9093',\n" +
    //     "    'properties.group.id' = '1391083',\n" +
    //     "    'debezium-json.schema-include' = 'true', \n" +
    //     "     'scan.startup.mode' = 'earliest-offset',\n" +
    //     "    'format'    = 'debezium-json'\n" +
    //     ")");

    // Historical example to group based on one tables changes over daily time x column aggregate
    // order revenue per product per minute

    // Note that this query does not check for updates every hour, it only checks the past hours when there is an update.
    // Therefore you will not automatically see results for last hour slots until an update is made during your current hour slot.
    // should be 2 between 12 and 1
    //tableEnv.executeSql("SELECT SUM(quantity) as `qty`, TUMBLE_START(event_time, interval '1' hour), TUMBLE_END(event_time, interval '1' hour) FROM orders group by TUMBLE(event_time, interval '1' hour);").print();

    // SOURCE
    // tableEnv.executeSql(
    //   "CREATE TABLE products_on_hand (\n" +
    //   "    product_id  INT,\n" +
    //   "    quantity INT,\n" +
    //   "    event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, \n" +
    //   "    WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND \n" +
    //   ") WITH (\n" +
    //   "    'connector' = 'kafka',\n" +
    //   "    'topic'     = 'eemmiillyyoo.inventory.products_on_hand',\n" +
    //   "    'properties.bootstrap.servers' = 'localhost:9093',\n" +
    //   "    'debezium-json.schema-include' = 'true', \n" +
    //   "    'properties.group.id' = '1391083',\n" +
    //   "    'scan.startup.mode' = 'earliest-offset',\n" +
    //   "    'format'    = 'debezium-json'\n" +
    //   ")"
    // );
    tableEnv.executeSql(params.get(SOURCE_KEY));

    // Table tbl = tableEnv.from("products_on_hand");
    Table tbl = tableEnv.from(params.get(TABLE_KEY));
    tbl.printSchema();

    // AGGREGATE
    // Table aggregate = tableEnv.sqlQuery(
    //   "SELECT SUM(quantity) as `summedQ` FROM products_on_hand;"
    // );
    Table aggregate = tableEnv.sqlQuery(params.get(QUERY_KEY));
    aggregate.printSchema();

    // SINK
    // tableEnv.executeSql(
    //   "CREATE TABLE products_on_hand_output (\n" +
    //   "summedQ BIGINT\n" +
    //   ") WITH (\n" +
    //   "'connector' = 'kafka',\n" +
    //   "'topic' = 'eemmiillyyoo.inventory.products_on_hand_output',\n" +
    //   "'properties.bootstrap.servers' = 'localhost:9093',\n" +
    //   "'format' = 'debezium-json'\n" +
    //   ")"
    // );
    tableEnv.executeSql(params.get(SINK_KEY));
    // add view result to topic
    aggregate.executeInsert(params.get(TABLE_KEY) + "_output");
  }
}
