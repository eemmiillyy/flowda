package flow.benchmark.utils;

import java.sql.DriverManager;
import java.sql.SQLException;

public class MysqlClient {

  java.sql.Connection conn = null;

  //   mysql://debezium:dbz@10.0.6.115:3306/inventory
  // mysql://debezium:dbz@10.0.6.115:3307/inventory

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

  public class seedSerial implements Runnable {

    private volatile MysqlClient mysqlClient;
    private volatile int iterations = 0;
    private volatile boolean killed = false;

    public seedSerial(MysqlClient mysqlClient) {
      this.mysqlClient = mysqlClient;
    }

    public void run() {
      System.out.println("new thread");
      try {
        while (!killed) {
          this.iterations = this.iterations + 1;
          System.out.println("looping " + this.iterations);
          mysqlClient.runQuery(
            String.format(
              "UPDATE products_on_hand    SET quantity=%s   WHERE product_id=109;",
              this.iterations
            )
          );
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
