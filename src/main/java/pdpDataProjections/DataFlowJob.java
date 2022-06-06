package pdpDataProjections;

import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableEnvironment;


public class DataFlowJob 
{
    public static void main( String[] args ) 
    {
        System.out.println( "App runninggg" );
        /**
         * Catalog connect to external systems for reading and writing. 
         * Tables are registered to the catalog. 
         * Tables can be sources or sinks.
         */
        // Set up source
        // System.out.println( "App running" );
        EnvironmentSettings env = EnvironmentSettings.newInstance().build();
        TableEnvironment tableEnv = TableEnvironment.create(env);
        
        tableEnv.executeSql("CREATE TABLE kafka_source (\n" +
            "    id  BIGINT,\n" +
            "    first_name VARCHAR,\n" +
            "    last_name VARCHAR\n" +
            ") WITH (\n" +
            "    'connector' = 'kafka',\n" +
            "    'topic'     = 'dbserver1.inventory.customers',\n" +
            "    'properties.bootstrap.servers' = 'localhost:9093',\n" +
            "    'properties.group.id' = '1391083',\n" +
            "     'scan.startup.mode' = 'earliest-offset',\n" +
            "    'format'    = 'json'\n" +
            ")");

        tableEnv.executeSql("SELECT COUNT(*) AS count_alias\n" +
         " FROM kafka_source\n" +
         " GROUP BY id;").print();
    }
}
