package flow.flink.job;

import org.apache.flink.api.java.utils.MultipleParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableEnvironment;

public class FlinkJob {

  public static void main(String[] args) {
    // ARGS
    final String SOURCE = "source";
    final String SOURCE_TWO = "sourceTwo";
    final String QUERY = "query";
    final String SINK = "sink";
    final String OUTPUT_TABLE_NAME = "table";

    MultipleParameterTool params = MultipleParameterTool.fromArgs(args);
    CustomLogger customLogger = new CustomLogger();
    // CHECK ARGS
    if (
      !params.has(SOURCE) ||
      !params.has(SOURCE_TWO) ||
      !params.has(QUERY) ||
      !params.has(SINK) ||
      !params.has(OUTPUT_TABLE_NAME)
    ) {
      customLogger.error(
        "exception: need parameters for jobs, e.g. '[COMMAND] --source test --query test --sink test --tablename test'"
      );
    } else {}

    // TODO register tableEnv to a single catalog.
    /**
     * Catalog connect to external systems for reading and writing.
     * Tables are registered to the catalog.
     * Tables can be sources or sinks.
     */
    EnvironmentSettings env = EnvironmentSettings.newInstance().build();
    TableEnvironment tableEnv = TableEnvironment.create(env);
    Configuration configuration = tableEnv.getConfig().getConfiguration();
    configuration.setString("table.optimizer.agg-phase-strategy", "ONE_PHASE");
    configuration.setString("sql-client.execution.result-mode", "CHANGELOG");
    // SOURCE ONE
    tableEnv.executeSql(params.get(SOURCE));
    // SOURCE TWO
    tableEnv.executeSql(params.get(SOURCE_TWO));

    // AGGREGATE
    Table aggregate = tableEnv.sqlQuery(params.get(QUERY));
    aggregate.printSchema();

    // SINK
    tableEnv.executeSql(params.get(SINK));
    // ADD AGGREGATE RESULT TO SINK TABLE
    aggregate.executeInsert(params.get(OUTPUT_TABLE_NAME));
  }
}
