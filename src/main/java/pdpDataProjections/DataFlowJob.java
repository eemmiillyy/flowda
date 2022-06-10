package pdpDataProjections;

import org.apache.flink.api.java.utils.MultipleParameterTool;
import org.apache.flink.core.fs.Path;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableEnvironment;


public class DataFlowJob 
{
    public static void main( String[] args ) 
    {
        // Args
        final String TOPIC_NAME_KEY = "topicName"; //environment
        final String SCHEMA_CONTENTS_KEY = "schemaContents";
        final String SQL_QUERY_KEY = "sqlQuery";

        MultipleParameterTool params = MultipleParameterTool.fromArgs(args);

        //if args is empty throw error
        if (!params.has(TOPIC_NAME_KEY) || !params.has(SCHEMA_CONTENTS_KEY) || !params.has(SQL_QUERY_KEY)) {
            System.out.println("ERROR: need parameters for jobs, e.g. '[COMMAND] --topicName test --schemaContents test --sqlQuery test'");
        } else {
            System.out.println(params.get(TOPIC_NAME_KEY));
            System.out.println(params.get(SCHEMA_CONTENTS_KEY));
            System.out.println(params.get(SQL_QUERY_KEY));
        }

        System.out.println( "App runninggg" );
        // TODO register tableEnv to a single catalog.
        /**
         * Catalog connect to external systems for reading and writing. 
         * Tables are registered to the catalog. 
         * Tables can be sources or sinks.
         */
        // Set up source
        // System.out.println( "App running" );
        // EnvironmentSettings env = EnvironmentSettings.newInstance().build();
        // TableEnvironment tableEnv = TableEnvironment.create(env);
        
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

         // Historical example to group based on one tables changes over daily time x column
         // order revenue per product per minute
     
        // Note that this query does not check for updates every hour, it only checks the past hours when there is an update. 
        // Therefore you will not automatically see results for last hour slots until an update is made during your current hour slot.
        // should be 2 between 12 and 1 
        //tableEnv.executeSql("SELECT SUM(quantity) as `qty`, TUMBLE_START(event_time, interval '1' hour), TUMBLE_END(event_time, interval '1' hour) FROM orders group by TUMBLE(event_time, interval '1' hour);").print();

        // tableEnv.executeSql("CREATE TABLE products_on_hand (\n" +
        // "    product_id  BIGINT,\n" +
        // "    quantity BIGINT,\n" +
        // "    event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, \n" +
        // "    WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND \n" +
        // ") WITH (\n" +
        // "    'connector' = 'kafka',\n" +
        // "    'topic'     = 'dbserver1.inventory.products_on_hand',\n" +
        // "    'properties.bootstrap.servers' = 'localhost:9093',\n" +
        // "    'debezium-json.schema-include' = 'true', \n" +
        // "    'properties.group.id' = '1391083',\n" +
        // "    'scan.startup.mode' = 'earliest-offset',\n" +
        // "    'format'    = 'debezium-json'\n" +
        // ")");


        // Cumulative sum example
       // tableEnv.executeSql("SELECT SUM(quantity) as summedQ FROM products_on_hand;").print();
    }
}

