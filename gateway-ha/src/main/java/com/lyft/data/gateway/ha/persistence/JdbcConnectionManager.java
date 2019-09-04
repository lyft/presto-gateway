package com.lyft.data.gateway.ha.persistence;

import com.lyft.data.gateway.ha.config.DataStoreConfiguration;
import com.lyft.data.gateway.ha.persistence.dao.QueryHistory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.Base;

@Slf4j
public class JdbcConnectionManager {
  private final DataStoreConfiguration configuration;
  private final ScheduledExecutorService executorService =
      Executors.newSingleThreadScheduledExecutor();

  public JdbcConnectionManager(DataStoreConfiguration configuration) {
    this.configuration = configuration;
    startCleanUps();
  }

  public void open() {
    Base.open(
        configuration.getDriver(),
        configuration.getJdbcUrl(),
        configuration.getUser(),
        configuration.getPassword());
    log.debug("Connection opened");
  }

  public void close() {
    Base.close();
    log.debug("Connection closed");
  }

  private void startCleanUps() {
    executorService.scheduleWithFixedDelay(
        () -> {
          log.info("Performing query history cleanup task");
          try {
            this.open();
            QueryHistory.delete(
                "created < ?", System.currentTimeMillis() - TimeUnit.HOURS.toMillis(4));
          } finally {
            this.close();
          }
        },
        1,
        120,
        TimeUnit.MINUTES);
  }
}
