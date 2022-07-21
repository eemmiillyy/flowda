package flow.flink.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomLogger {

  private static final Logger LOG = LoggerFactory.getLogger(CustomLogger.class);

  public void info(String args) {
    LOG.info("{}", args);
  }

  public void error(String args) {
    LOG.error("{}", args);
  }
}
