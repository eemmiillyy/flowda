package userSource;

public class Server {

  public static void main(String[] strings) {
    Bootstrap bootstrap = new Bootstrap();
    bootstrap.start();
    System.out.println("Number of threads in main" + Thread.activeCount());
  }
}
