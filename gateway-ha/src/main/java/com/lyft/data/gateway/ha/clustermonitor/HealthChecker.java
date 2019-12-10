package com.lyft.data.gateway.ha.clustermonitor;

import com.lyft.data.gateway.ha.notifier.Notifier;
import java.util.List;

public class HealthChecker implements PrestoClusterStatsObserver {
  private static final int MAX_THRESHOLD_QUEUED_QUERY_COUNT = 100;
  private Notifier notifier;

  public HealthChecker(Notifier notifier) {
    this.notifier = notifier;
  }

  @Override
  public void observe(List<ClusterStats> clustersStats) {
    for (ClusterStats clusterStats : clustersStats) {
      if (!clusterStats.isHealthy()) {
        notifyUnhealthyCluster(clusterStats);
      } else {
        if (clusterStats.getQueuedQueryCount() > MAX_THRESHOLD_QUEUED_QUERY_COUNT) {
          notifyForTooManyQueuedQueries(clusterStats);
        }
        if (clusterStats.getNumWorkerNodes() < 1) {
          notifyForNoWorkers(clusterStats);
        }
      }
    }
  }

  private void notifyUnhealthyCluster(ClusterStats clusterStats) {
    notifier.sendNotification(String.format("%s - Cluster unhealthy",
        clusterStats.getClusterId()),
        clusterStats.toString());
  }

  private void notifyForTooManyQueuedQueries(ClusterStats clusterStats) {
    notifier.sendNotification(String.format("%s - Too many queued queries",
        clusterStats.getClusterId()), clusterStats.toString());
  }

  private void notifyForNoWorkers(ClusterStats clusterStats) {
    notifier.sendNotification(String.format("%s - Number of workers",
        clusterStats.getClusterId()), clusterStats.toString());
  }


}
