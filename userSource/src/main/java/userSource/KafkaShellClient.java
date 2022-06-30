package userSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Requires knowing the name of the docker container kafka is running in ahead of time.
 * docker exec pdpdataprojections_kafka_1 bash -c "ls -la" for individual commands
 * docker exec -i pdpdataprojections_kafka_1 bash < src/main/java/userSource/testscript.sh to execute a local script
 * Configs are under /opt/bitnami/kafka in bitnami
 */
public class KafkaShellClient {

  /*
   * Runs a command against the kafka cluster from the host machine.
   * Assumes this service and kafka are running on the same host machine with kafka runnning in docker.
   *
   */
  public void run(String command) throws IOException, InterruptedException {
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

      // int exitVal = process.waitFor();
      System.out.println(output);
      // if (exitVal != 0) {
      //   throw new IOException();
      // }
    } catch (IOException e) {
      throw e;
    }
  }

  // TODO dynamically get the name of the container and the path to the kafka bin
  public void addACLUser(String environmentId, String password)
    throws IOException, InterruptedException {
    String command = String.format(
      "docker exec -u root pdpdataprojections_kafka_1 bash -c \"cd opt/bitnami/kafka/bin && kafka-configs.sh --zookeeper zookeeper:2181 --alter --add-config 'SCRAM-SHA-256=[iterations=8192,password=%s],SCRAM-SHA-512=[password=%s]' --entity-type users --entity-name %s\"",
      password,
      password,
      environmentId
    );
    run(command);
  }

  /**
   * Add ACL rule for given user and topic. Creates access for all operations on all topics with this prefix
   *
   */
  public void addACLRule(String environmentId)
    throws IOException, InterruptedException {
    String command = String.format(
      "docker exec -u root pdpdataprojections_kafka_1 bash -c \"cd opt/bitnami/kafka/bin && kafka-acls.sh --authorizer-properties zookeeper.connect=zookeeper:2181 --add --allow-principal User:%s --operation ALL --topic \"%s\" --resource-pattern-type PREFIXED\"",
      environmentId,
      environmentId
    );
    run(command);
  }

  public void addACLRuleConsumer(String environmentId)
    throws IOException, InterruptedException {
    String command = String.format(
      "docker exec -u root pdpdataprojections_kafka_1 bash -c \"cd opt/bitnami/kafka/bin && kafka-acls.sh --authorizer-properties zookeeper.connect=zookeeper:2181 --add --allow-principal User:%s --operation ALL --group %s\"",
      environmentId,
      environmentId
    );
    run(command);
  }
}
