package userSource.Settings;

public class SettingsShape {

  public Stage stage;

  public class Stage {

    public StageInstance production;
    public StageInstance development;
    public StageInstance test;

    public class StageInstance {

      public Services services;

      public class Services {

        public Kafka kafka;

        public class Kafka {

          public Admin admin;
          public Bootstrap bootstrap;
          public Sasl sasl;
        }

        public class Admin {

          public String user;
          public String $$password;
        }

        public class Bootstrap {

          public String containerName;
          public String serversInternal;
          public String serversExternal;
          public String pathToBin;
        }

        public class Sasl {

          public String mechanism;
          public String protocol;
        }

        public Flink flink;

        public class Flink {

          public String servers;
          public String jar;
        }

        public Debezium debezium;

        public class Debezium {

          public String containerName;
          public String servers;
        }

        public Zookeeper zookeeper;

        public class Zookeeper {

          public String containerName;
          public String serversExternal;
          public String serversInternal;
        }
      }
    }
  }
}
