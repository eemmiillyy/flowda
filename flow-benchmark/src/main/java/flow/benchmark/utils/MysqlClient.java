package flow.benchmark.utils;

import java.sql.DriverManager;
import java.sql.SQLException;

public class MysqlClient {

  java.sql.Connection conn = null;

  public MysqlClient(String connection, String user, String pass)
    throws Exception {
    Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
    try {
      conn = DriverManager.getConnection("jdbc:" + connection, user, pass);
    } catch (SQLException e) {
      System.out.println("Could not connect to server");
      throw e;
    }
  }

  public void runQuery(String query) throws SQLException {
    try {
      conn.createStatement().executeUpdate(query);
    } catch (SQLException e) {
      System.out.println("Could not run update query" + e);
      throw e;
    }
  }

  public class seedSerial implements Runnable {

    private volatile MysqlClient mysqlClient;
    private volatile int iterations = 0;
    private volatile boolean killed = false;
    private volatile String sqlString;

    public seedSerial(MysqlClient mysqlClient, String sqlString) {
      this.mysqlClient = mysqlClient;
      this.sqlString = sqlString;
    }

    public void run() {
      try {
        while (!killed) {
          this.iterations = this.iterations + 1;
          mysqlClient.runQuery(String.format(sqlString, this.iterations));
        }
        System.out.println(
          "killed, seeded" + iterations + "records successfully."
        );
      } catch (SQLException e) {
        System.out.println("Issue running seed");
      }
    }

    public void kill() {
      killed = true;
    }
  }
}
