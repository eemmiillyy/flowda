package userSource.Kafka;

import java.util.Properties;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;

import userSource.Settings.Settings;
import userSource.Settings.SettingsShape.Stage.StageInstance;

public class KafkaClient {

  private String kafkaUser;
  private String kafkaUserApiKey;
  public Settings settings;

  /**
   *
   * Creates a kafka client for the given
   */
  public KafkaClient(
    String kafkaUser,
    String kafkaUserApiKey,
    Settings settings
  ) {
    this.kafkaUser = kafkaUser;
    this.kafkaUserApiKey = kafkaUserApiKey;
    this.settings = settings;
  }

  public KafkaConsumer<String, String> create(String environmentId) {
    // Random group id for client
    // TODO If there is NOT already an existing client then create a new one

    System.out.println("trying to create consumer");
    StageInstance stage = this.settings.settings;

    String login = String.format(
      "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";",
      kafkaUser,
      kafkaUserApiKey
    );
    // Create kafka client
    Properties props = new Properties();
    props.put(
      "bootstrap.servers",
      stage.services.kafka.bootstrap.serversExternal
    );
    props.put("group.id", this.kafkaUser);
    props.put("key.deserializer", StringDeserializer.class.getName());
    props.put("value.deserializer", StringDeserializer.class.getName());
    props.put("auto.offset.reset", "earliest");
    props.put("security.protocol", stage.services.kafka.sasl.protocol);
    props.put(SaslConfigs.SASL_MECHANISM, stage.services.kafka.sasl.mechanism);
    props.put(SaslConfigs.SASL_JAAS_CONFIG, login);

    return new KafkaConsumer<>(props);
  }
}
