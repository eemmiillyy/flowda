package flow.core.Utils;

import java.net.URI;

public class ConnectionStringParser {

  public class ConnectionStringParsed {

    public String host;
    public String port;
    public String dbName;
    public String username;
    public String password;

    public ConnectionStringParsed(
      String host,
      String port,
      String dbName,
      String username,
      String password
    ) {
      this.host = host;
      this.dbName = dbName;
      this.username = username;
      this.password = password;
      this.port = port;
    }
  }

  public ConnectionStringParsed parse(String connectionString) {
    URI uri = URI.create(connectionString);
    String[] userInfo = uri.getUserInfo().split(":");
    String host = uri.getHost();
    Number port = uri.getPort();
    // TODO check that they can submit a string without a db name
    // Remove / at beginning of db name
    String dbName = uri.getPath().substring(1);
    String username = userInfo[0];
    String password = userInfo[1];
    return new ConnectionStringParsed(
      host,
      port.toString(),
      dbName,
      username,
      password
    );
  }
}
