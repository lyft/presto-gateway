package com.lyft.data.gateway.ha.clustermonitor;

import com.lyft.data.gateway.ha.config.DataStoreConfiguration;
import com.lyft.data.gateway.ha.notifier.Notifier;
import com.lyft.data.gateway.ha.persistence.JdbcConnectionManager;
import com.lyft.data.gateway.ha.router.GatewayBackendStateManager.GatewayBackendState;
import com.lyft.data.gateway.ha.router.HaGatewayBackendStateManager;
import java.util.List;


public class HealthChecker implements PrestoClusterStatsObserver {
  private static final int MAX_THRESHOLD_QUEUED_QUERY_COUNT = 100;
  private Notifier notifier;
  private HaGatewayBackendStateManager haGatewayBackendStateManager;

  public HealthChecker(Notifier notifier) {
    this.notifier = notifier;
  }

  @Override
  public void observe(List<ClusterStats> clustersStats) {
    for (ClusterStats clusterStat : clustersStats) {
      // TODO: Store the stats in a persistent layer
      GatewayBackendState backend = new GatewayBackendState();
      backend.setName(clusterStat.getClusterId());
      backend.setHealth(clusterStat.getHealthy());
      backend.setWorkerCount(clusterStat.getNumWorkerNodes());
      GatewayBackendState updated = haGatewayBackendStateManager.addBackend(backend);
    }
  }
}
