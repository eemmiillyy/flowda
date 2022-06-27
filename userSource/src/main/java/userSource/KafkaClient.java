package userSource;

import java.util.Properties;
import java.util.Random;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;

public class KafkaClient {

  public KafkaConsumer<String, String> create() {
    // Random group id for client
    // TODO If there is NOT already an existing client then create a new one
    Random rand = new Random();
    Integer value = rand.nextInt(10000) + 1;
    String groupId = value.toString();
    System.out.println(groupId);

    // Create kafka client
    Properties props = new Properties();
    props.put("bootstrap.servers", "localhost:9093");
    props.put("group.id", groupId);
    props.put("key.deserializer", StringDeserializer.class.getName());
    props.put("value.deserializer", StringDeserializer.class.getName());
    props.put("auto.offset.reset", "earliest");
    props.put("security.protocol", "SASL_PLAINTEXT");
    props.put(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-256");
    props.put(
      SaslConfigs.SASL_JAAS_CONFIG,
      "org.apache.kafka.common.security.scram.ScramLoginModule required username=emily password=bleepbloop;"
    );

    // "database.history.consumer.security.protocol":"SASL_PLAINTEXT",
    // "database.history.producer.security.protocol":"SASL_PLAINTEXT",
    // "database.history.consumer.sasl.mechanism":"SCRAM-SHA-512",
    // "database.history.producer.sasl.mechanism":"SCRAM-SHA-512",
    // "database.history.consumer.sasl.jaas.config":"org.apache.kafka.common.security.scram.ScramLoginModule required username=\"emily\" password=\"bleepbloop\";",
    // "database.history.producer.sasl.jaas.config":"org.apache.kafka.common.security.scram.ScramLoginModule required username=\"emily\" password=\"bleepbloop\";",

    return new KafkaConsumer<>(props);
  }
}
