package flow.benchmark;

public class BenchmarkRunner {

  /**
   *
   * Each benchmark type (simple vs complex) needs a job, and database respectively.
   */
  public static void main(String[] args) throws Exception {
    String environmentIdSimple = "simple";
    new Throughput(
      environmentIdSimple,
      "UPDATE products_on_hand SET quantity=0 WHERE !isnull (product_id);",
      "UPDATE products_on_hand SET quantity=%s WHERE product_id=109;",
      5,
      15,
      "simple",
      "mysql://mysqluser:mysqlpw@127.0.0.1:3306/inventory",
      "mysqluser",
      "mysqlpw"
    );
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
    String environmentIdComplex = "complex";
    new Throughput(
      environmentIdComplex,
      "UPDATE orders SET quantity=0 WHERE !isnull (order_number);",
      "UPDATE orders SET quantity=%s WHERE product_id=102;",
      5,
      15,
      "complex",
      "mysql://mysqluser:mysqlpw@127.0.0.1:3307/inventory",
      "mysqluser",
      "mysqlpw"
    );
    new Latency(
      environmentIdComplex,
      "UPDATE orders SET quantity=0 WHERE !isnull (order_number);",
      "UPDATE orders SET quantity=%s WHERE product_id=102;",
      5,
      15,
      "complex",
      "{\"before\":null,\"after\":{\"email\":\"sally.thomas@acme.com\",\"orderCount\":1,\"orderQuantity\":%s},\"op\":\"c\"}",
      "mysql://mysqluser:mysqlpw@127.0.0.1:3307/inventory",
      "mysqluser",
      "mysqlpw"
    );
  }
}
