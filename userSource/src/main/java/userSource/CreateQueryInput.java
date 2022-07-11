package userSource;

public class CreateQueryInput {

  public String connectionString; // TODO Switch this to JWT in header
  public String environmentId; // or dbServerName, encode in JWT
  public String databaseName;
  public String tableName;
  public String fieldName;
}
