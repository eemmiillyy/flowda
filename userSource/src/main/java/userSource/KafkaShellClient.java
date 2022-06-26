package userSource;

/**
 * Requires knowing the name of the docker container kafka is running in ahead of time.
 * docker exec pdpdataprojections_kafka_1 bash -c "ls -la" for individual commands
 * docker exec -i pdpdataprojections_kafka_1 bash < src/main/java/userSource/testscript.sh to execute a local script
 * Configs are under /opt/bitnami/kafka in bitnami
 * docker exec pdpdataprojections_kafka_1 bash -c "/opt/bitnami/kafka/kafka-configs.sh --zookeeper localhost:2181 --alter --add-config 'SCRAM-SHA-256=[iterations=8192,password=emily-secret],SCRAM-SHA-512=[password=emily-secret]' --entity-type users --entity-name emily"
 */
public class KafkaShellClient {

  public String generateAccessKey() {
    return "";
  }

  /*
   * Interacts with the kafka cluster from the host machine.
   * Assumes this service and kafka are running on the same host machine,
   * with kafka in docker
   *
   * bin/kafka-configs.sh --zookeeper localhost:2181 --alter --add-config 'SCRAM-SHA-256=[iterations=8192,password=emily-secret],SCRAM-SHA-512=[password=emily-secret]' --entity-type users --entity-name emily
   */
  public void addACLUser(String environmentId, String password) {}

  /**
   * bin/kafka-acls.sh --authorizer kafka.security.auth.SimpleAclAuthorizer --authorizer-properties zookeeper.connect=10.2.36.42:2181,10.2.36.149:2181,10.2.36.168:2181 --add --allow-principal User:producer --operation Write --topic test --allow-host 192.168.2.*
   *
   */
  public void addACLRule(String environment) {}
}
