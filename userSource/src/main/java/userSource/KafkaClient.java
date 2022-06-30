package userSource;

import java.util.Properties;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;

public class KafkaClient {

  private String kafkaUser;
  private String kafkaUserApiKey;

  /**
   *
   * Creates a kafka client for the given
   */
  public KafkaClient(String kafkaUser, String kafkaUserApiKey) {
    this.kafkaUser = kafkaUser;
    this.kafkaUserApiKey = kafkaUserApiKey;
  }

  public KafkaConsumer<String, String> create(String environmentId) {
    // Random group id for client
    // TODO If there is NOT already an existing client then create a new one

    String test = String.format(
      "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";",
      kafkaUser,
      kafkaUserApiKey
    );
    System.out.println(test);
    // Create kafka client
    Properties props = new Properties();
    props.put("bootstrap.servers", "localhost:9093");
    props.put("group.id", this.kafkaUser);
    props.put("key.deserializer", StringDeserializer.class.getName());
    props.put("value.deserializer", StringDeserializer.class.getName());
    props.put("auto.offset.reset", "earliest");
    props.put("security.protocol", "SASL_PLAINTEXT");
    props.put(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-256");
    props.put(SaslConfigs.SASL_JAAS_CONFIG, test);

    return new KafkaConsumer<>(props);
  }
}
