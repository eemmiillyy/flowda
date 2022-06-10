package pdpDataProjections;

/**
 * 
 * Given this payload ddl from the server kafka topic after connecting with debezium.
 * Sub the create statement input datatype.
 * 
 * @param model an array of the models to find
 * @return       the transformed prisma schema into a kafka connect acceptable format.  
 * 
 * https://nightlies.apache.org/flink/flink-docs-release-1.11/dev/table/types.html
 * https://nightlies.apache.org/flink/flink-docs-release-1.11/dev/table/sql/create.html
 * https://www.prisma.io/docs/concepts/database-connectors/mysql
 * 
 * e.g. 
 * 
 * Input:
 * CREATE TABLE `customers` (\n  `id` int NOT NULL AUTO_INCREMENT,\n  `first_name` varchar(255) NOT NULL,\n  `last_name` varchar(255) NOT NULL,\n  `email` varchar(255) NOT NULL,\n  PRIMARY KEY (`id`),\n  UNIQUE KEY `email` (`email`)\n) ENGINE=InnoDB AUTO_INCREMENT=1005 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
 * 
 * Output:
 * 
 * CREATE TABLE `customers` 
 * (\n  `id` int,
 *  `first_name` varchar,
 * `last_name` varchar,
 *  `email` varchar,
 * "event_time TIMESTAMP(3) METADATA FROM 'value.source.timestamp' VIRTUAL, \n" 
 * "WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND \n" +
"
 */
public class Converter {
    public String substitueDataTypes(String model) {
        // Look for schema change, where payload.source.db is correct, payload.source.table is the one from the query, and payload.source.ddl contains "CREATE. Then rebuild the query by looping through the tableChanges[0].columns JSON. 
        // send that to the job as a parameter. 
        return model;
    }
}
