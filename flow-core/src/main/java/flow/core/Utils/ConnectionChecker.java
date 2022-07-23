package flow.core.Utils;

import java.sql.DriverManager;
import java.sql.SQLException;

import flow.core.Utils.ConnectionStringParser.ConnectionStringParsed;

public class ConnectionChecker {

  public class AccessDeniedError extends RuntimeException {

    public AccessDeniedError(String arg) {
      super(arg);
    }
  }

  public Boolean canConnect(String connectionString)
    throws SQLException, AccessDeniedError, ClassNotFoundException, InstantiationException, IllegalAccessException {
    ConnectionStringParsed connectionInfo = new ConnectionStringParser()
    .parse(connectionString);
    java.sql.Connection conn = null;
    Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
    try {
      conn =
        DriverManager.getConnection(
          "jdbc:" + connectionString,
          connectionInfo.username,
          connectionInfo.password
        );

      try {
        // See if the user is root
        conn.createStatement().executeQuery("SHOW BINARY LOGS;");
      } catch (SQLException e) {
        throw new AccessDeniedError(
          "You need (at least one of) the SUPER, REPLICATION CLIENT privilege(s) for this operation"
        );
      }
    } catch (SQLException e) {
      // Could not connect to server
      throw e;
    } finally {
      if (conn != null) {
        conn.close();
      }
    }

    return true;
  }
}
