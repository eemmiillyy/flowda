package userSource;

public class Server {

  public static void main(String[] strings) throws Exception {
    Bootstrap bootstrap = new Bootstrap("development");
    bootstrap.start();
  }
}
