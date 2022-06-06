package pdpDataProjections;

import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.catalog.Catalog;

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
    "    'properties.bootstrap.servers' = '192.168.112.4:9092',\n" +
    "    'properties.group.id' = 'testGroup2',\n" +
    "    'format'    = 'json'\n" +
    ")");

     // Output sink table storing final results of the aggregation. Stores it in 
     // an underlying MySQL database. 
      Table transactions = tableEnv.from("kafka_source");
    TableResult result = tableEnv.executeSql("SELECT * FROM kafka_source");
    //  System.out.println(result);
    }

    // public static Table report(Table transactions) {
    //     return transactions.select(
    //             $("account_id"),
    //             $("transaction_time").floor(TimeIntervalUnit.HOUR).as("log_ts"),
    //             $("amount"))
    //         .groupBy($("account_id"), $("log_ts"))
    //         .select(
    //             $("account_id"),
    //             $("log_ts"),
    //             $("amount").sum().as("amount"));
    // }
}
