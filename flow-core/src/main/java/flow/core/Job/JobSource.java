package flow.core.Job;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;

import flow.core.Settings.Settings;
import flow.core.Utils.ClassToStringConverter;

public class JobSource {

  private Settings settings;
  private Gson g;

  public JobSource(Settings settings) {
    this.settings = settings;

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
      this.settings.settings.services.kafka.bootstrap.serversInternal;

    ArrayList<String> map = new ClassToStringConverter()
    .convertToStringArray(connectionObject);
    return (content + "WITH (" + String.join(",", map) + ")");
  }
}
