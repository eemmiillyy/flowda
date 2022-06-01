package pdpDataProjections;

import org.apache.flink.api.common.eventtime.AscendingTimestampsWatermarks;
import org.apache.flink.api.common.eventtime.TimestampAssigner;
import org.apache.flink.api.common.eventtime.TimestampAssignerSupplier;
import org.apache.flink.api.common.eventtime.WatermarkGenerator;
import org.apache.flink.api.common.eventtime.WatermarkGeneratorSupplier;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;

public class SocketWindowWordCount {

    public static void main(String[] args) throws Exception {
        // Create the execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Obtain the input data by connecting to the socket. Here you want to connect to the local 9000 port. If 9000 port is not available, you'll need to change the port.
        DataStream<String> text = env.socketTextStream("localhost", 9000, "\n");
        env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);
        // Parse the data, group by word, and perform the window and aggregation operations.
        DataStream<Tuple2<String, Integer>> windowCounts = text
                .flatMap(new Tokenizer())
                .keyBy(value -> value.f0)
                .timeWindow(Time.seconds(5))
                .sum(1);

        // Print the results to the console. Note that here single-threaded printed is used, rather than multi-threading.
        windowCounts.print().setParallelism(1);

        env.execute("Socket Window WordCount");
    }

    public static final class Tokenizer
    implements FlatMapFunction<String, Tuple2<String, Integer>> {

@Override
public void flatMap(String value, Collector<Tuple2<String, Integer>> out) {
    // normalize and split the line
    String[] tokens = value.toLowerCase().split("\\W+");

    // emit the pairs
    for (String token : tokens) {
        if (token.length() > 0) {
            out.collect(new Tuple2<>(token, 1));
        }
    }
}
}

private static class IngestionTimeWatermarkStrategy<T> implements WatermarkStrategy<T> {

    private IngestionTimeWatermarkStrategy() {}

    @Override
    public WatermarkGenerator<T> createWatermarkGenerator(
            WatermarkGeneratorSupplier.Context context) {
        return new AscendingTimestampsWatermarks<>();
    }

    @Override
    public TimestampAssigner<T> createTimestampAssigner(
            TimestampAssignerSupplier.Context context) {
                   return (event, timestamp) -> System.currentTimeMillis();

                
    }
}
}