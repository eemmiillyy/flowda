package flow.benchmark;

public class BenchmarkRunner {

  /**
   *
   * Each benchmark type (simple vs complex) needs a job, and database respectively.
   */
  public static void main(String[] args) throws Exception {
    String environmentIdSimple = "simple";
    new Latency(
      environmentIdSimple,
      "UPDATE products_on_hand SET quantity=0 WHERE !isnull (product_id);",
      "UPDATE products_on_hand SET quantity=%s WHERE product_id=109;",
      5,
      15,
      "simple",
      "{\"before\":null,\"after\":{\"summed\":%s},\"op\":\"c\"}",
      "mysql://mysqluser:mysqlpw@127.0.0.1:3306/inventory",
      "mysqluser",
      "mysqlpw"
    );
    // new Throughput(
    //   environmentIdSimple,
    //   "UPDATE products_on_hand SET quantity=0 WHERE !isnull (product_id);",
    //   "UPDATE products_on_hand SET quantity=%s WHERE product_id=109;",
    //   5,
    //   15,
    //   "simple",
    //   "mysql://mysqluser:mysqlpw@127.0.0.1:3306/inventory",
    //   "mysqluser",
    //   "mysqlpw"
    // );
  }
}
