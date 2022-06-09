package pdpDataProjections;

import java.util.Arrays;

import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableEnvironment;


public class DataFlowJob 
{
    public static void main( String[] args ) 
    {
        System.out.println( "App runninggg" );
        // TODO register tableEnv to a single catalog.
        /**
         * Catalog connect to external systems for reading and writing. 
         * Tables are registered to the catalog. 
         * Tables can be sources or sinks.
         */
        // Set up source
        // System.out.println( "App running" );
        EnvironmentSettings env = EnvironmentSettings.newInstance().build();
        TableEnvironment tableEnv = TableEnvironment.create(env);
        
        // tableEnv.executeSql("CREATE TABLE kafka_source (\n" +
        //     "    id  BIGINT,\n" +
        //     "    first_name VARCHAR,\n" +
        //     "    last_name VARCHAR\n" +
        //     ") WITH (\n" +
        //     "    'connector' = 'kafka',\n" +
        //     "    'topic'     = 'dbserver1.inventory.customers',\n" +
        //     "    'properties.bootstrap.servers' = 'localhost:9093',\n" +
        //     "    'properties.group.id' = '1391083',\n" +
        //     "     'scan.startup.mode' = 'earliest-offset',\n" +
        //     "    'format'    = 'json'\n" +
        //     ")");

        tableEnv.executeSql("CREATE TABLE kafka_source (\n" +
        "    product_id  BIGINT,\n" +
        "    quantity BIGINT,\n" +
        "    event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL \n" +
        ") WITH (\n" +
        "    'connector' = 'kafka',\n" +
        "    'topic'     = 'dbserver1.inventory.products_on_hand',\n" +
        "    'properties.bootstrap.servers' = 'localhost:9093',\n" +
        "    'debezium-json.schema-include' = 'true', \n" +
        "    'properties.group.id' = '1391083',\n" +
        "     'scan.startup.mode' = 'earliest-offset',\n" +
        "    'format'    = 'debezium-json'\n" +
        ")");

        // tableEnv.executeSql("SELECT COUNT(*) AS count_alias\n" +
        //  " FROM kafka_source\n" +
        //  " GROUP BY id;").print();
       // tableEnv.executeSql("SELECT quantity AS quan FROM kafka_source GROUP BY product_id;").print();
        System.out.println(Arrays.toString(tableEnv.listCatalogs()));
        System.out.println(Arrays.toString(tableEnv.listDatabases()));
        System.out.println(Arrays.toString(tableEnv.listTables()));
        // tableEnv.executeSql("SELECT product_id, COUNT(quantity) as cnt\n" +
        //  " FROM kafka_source\n" +
        //  " GROUP BY product_id;").print();
        // Historical example to group based on one tables changes over daily time x column
        tableEnv.executeSql("SELECT product_id, TUMBLE_END(event_time, INTERVAL '10' MINUTE) AS endT, COUNT(quantity) as cnt FROM kafka_source GROUP BY product_id, TUMBLE(event_time, INTERVAL '10' MINUTE);").print();
        //tableEnv.executeSql("SELECT * FROM kafka_source;").print();
        //tableEnv.executeSql("SELECT product_id, SUM(quantity) as summedQ FROM kafka_source GROUP BY product_id;").print();
    }
}

