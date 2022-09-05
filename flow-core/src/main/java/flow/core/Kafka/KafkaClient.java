package flow.core.Kafka;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import flow.core.Settings.Settings;

public class KafkaClient {

  /*
   * Runs a command against the kafka cluster from the host machine.
   * Assumes this service and kafka are running on the same host machine with kafka runnning in docker.
   *
   */
  private Settings settings;

  public KafkaClient(Settings settings) {
    this.settings = settings;
  }

  public void modifyACL(String command)
    throws IOException, InterruptedException {
    ProcessBuilder processBuilder = new ProcessBuilder();

    try {
      processBuilder.command("bash", "-c", command);

      Process process = processBuilder.start();
      // Concatenate output
      StringBuilder output = new StringBuilder();
      // Read output stream from process
      BufferedReader reader = new BufferedReader(
        new InputStreamReader(process.getInputStream())
      );

      String line;
      while ((line = reader.readLine()) != null) {
        output.append(line + "\n");
      }

      int exitVal = process.waitFor();
      System.out.println(output);
      if (exitVal != 0) {
        throw new IOException();
      }
    } catch (IOException e) {
      throw e;
    }
  }

  public String createPermissions(String environmentId, String password) {
    String kafkaUser = createKafkaUser(environmentId, password);
    String topicAccess = createTopicAccess(environmentId);
    String groupAccess = createGroupAccess(environmentId);
    String stage = System.getenv("STAGE");

    return String.format(
      stage.equals("production")
        ? "sudo docker exec %s bash -c \"cd %s && %s && %s && %s\""
        : "docker exec %s bash -c \"cd %s && %s && %s && %s\"",
      this.settings.settings.services.kafka.bootstrap.containerName,
      this.settings.settings.services.kafka.bootstrap.pathToBin,
      kafkaUser,
      topicAccess,
      groupAccess
    );
  }

  public String createKafkaUser(String environmentId, String password) {
    return String.format(
      "kafka-configs.sh --zookeeper %s --alter --add-config 'SCRAM-SHA-256=[iterations=8192,password=%s],SCRAM-SHA-512=[password=%s]' --entity-type users --entity-name %s",
      this.settings.settings.services.zookeeper.serversInternal,
      password,
      password,
      environmentId
    );
  }

  public String createTopicAccess(String environmentId) {
    return String.format(
      "kafka-acls.sh --authorizer-properties zookeeper.connect=%s --add --allow-principal User:%s --operation ALL --topic \"%s.\" --resource-pattern-type PREFIXED",
      this.settings.settings.services.zookeeper.serversInternal,
      environmentId,
      environmentId
    );
  }

  public String createGroupAccess(String environmentId) {
    return String.format(
      "kafka-acls.sh --authorizer-properties zookeeper.connect=%s --add --allow-principal User:%s --operation ALL --group %s",
      this.settings.settings.services.zookeeper.serversInternal,
      environmentId,
      environmentId
    );
  }
}
