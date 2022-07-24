package flow.core.Job;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;

import flow.core.Settings.Settings;
import flow.core.Utils.ClassToStringConverter;

public class JobSource {

  private String loginModule;
  private Settings settings;
  private Gson g;

  public JobSource(Settings settings) {
    this.settings = settings;

    this.loginModule =
      String.format(
        "org.apache.kafka.common.security.scram.ScramLoginModule required username=%s password=%s;",
        this.settings.settings.services.kafka.admin.user,
        settings.decryptField(
          this.settings.settings.services.kafka.admin.$$password
        )
      );
    this.g = new Gson();
  }

  public String extractTableNameFromCreateStatement(String sourceSql) {
    String createStatement = "CREATE TABLE";
    Pattern pattern = Pattern.compile(createStatement + "\\W+(\\w+)");
    Matcher matcher = pattern.matcher(sourceSql);
    return matcher.find() ? matcher.group(1) : null;
  }

  public String build(
    Boolean source,
    String content,
    String databaseName,
    String environmentId,
    String tableName
  )
    throws Exception {
    JobConnectionBaseType connectionObject =
      this.g.fromJson(
          "{}",
          source ? JobConnectionSourceType.class : JobConnectionBaseType.class
        );

    connectionObject.topic =
      environmentId + "." + databaseName + "." + tableName;
    connectionObject.properties_bootstrap_servers =
      this.settings.settings.services.kafka.bootstrap.serversExternal;
    connectionObject.properties_group_id = environmentId;
    connectionObject.properties_sasl_mechanism =
      this.settings.settings.services.kafka.sasl.mechanism;
    connectionObject.properties_security_protocol =
      this.settings.settings.services.kafka.sasl.protocol;
    connectionObject.properties_sasl_jaas_config = this.loginModule;

    ArrayList<String> map = new ClassToStringConverter()
    .convertToStringArray(connectionObject);
    return (content + "WITH (" + String.join(",", map) + ")");
  }
}
