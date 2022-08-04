package flow.benchmark.utils;

import java.sql.DriverManager;
import java.sql.SQLException;

public class MysqlClient {

  java.sql.Connection conn = null;

  public MysqlClient() throws Exception {
    Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
    try {
      conn =
        DriverManager.getConnection(
          "jdbc:" + "mysql://mysqluser:mysqlpw@127.0.0.1:3306/inventory",
          "mysqluser",
          "mysqlpw"
        );
    } catch (SQLException e) {
      // Could not connect to server
      System.out.println("Could not connect to server");
      throw e;
    }
  }

  public void runQuery(String query) throws SQLException {
    try {
      int result = conn.createStatement().executeUpdate(query);
      //   System.out.println(result);
    } catch (SQLException e) {
      System.out.println("Could not run update query");
      throw e;
    }
  }
}
