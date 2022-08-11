package flow.benchmark.utils;

import java.util.Properties;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;

public class KafkaClient {

  /**
   *
   * Creates a kafka client for the given
   */

  public KafkaConsumer<String, String> create(String groupId) throws Exception {
    // Ensure each kafka consumer belongs to a new consumer group
    String randomGroupId = groupId;

    // TODO take from settings
    String login = String.format(
      "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";",
      "emily",
      "bleepbloop"
    );
    // Create kafka client
    Properties props = new Properties();
    props.put("bootstrap.servers", "localhost:9093");
    props.put("group.id", randomGroupId);
    props.put("key.deserializer", StringDeserializer.class.getName());
    props.put("value.deserializer", StringDeserializer.class.getName());
    props.put("auto.offset.reset", "earliest");
    props.put("security.protocol", "SASL_PLAINTEXT");
    props.put("max.poll.records", 100000);
    props.put("max.partition.fetch.bytes", 100000);
    props.put("default.api.timeout.ms", 6000);
    props.put(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-256");
    props.put(SaslConfigs.SASL_JAAS_CONFIG, login);

    return new KafkaConsumer<>(props);
  }
}
