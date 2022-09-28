package com.lyft.data.gateway.ha.clustermonitor;

import com.lyft.data.gateway.ha.router.PrestoQueueLengthRoutingTable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Updates the QueueLength Based Routing Manager {@link PrestoQueueLengthRoutingTable} with
 * updated queue lengths of active clusters.
 */
public class PrestoQueueLengthChecker implements PrestoClusterStatsObserver {

  PrestoQueueLengthRoutingTable routingManager;

  public PrestoQueueLengthChecker(PrestoQueueLengthRoutingTable routingManager) {
    this.routingManager = routingManager;
  }

  @Override
  public void observe(List<ClusterStats> stats) {
    Map<String, Map<String, Integer>> clusterQueueMap = new HashMap<String, Map<String, Integer>>();
    Map<String, Map<String, Integer>> clusterRunningMap
            = new HashMap<String, Map<String, Integer>>();
    Map<String, Map<String, Integer>> userClusterQueuedCount
            = new HashMap<>();

    for (ClusterStats stat : stats) {
      if (!stat.isHealthy()) {
        // Skip if the cluster isn't healthy
        continue;
      }
      if (!clusterQueueMap.containsKey(stat.getRoutingGroup())) {
        clusterQueueMap.put(stat.getRoutingGroup(), new HashMap<String, Integer>() {
              {
                put(stat.getClusterId(), stat.getQueuedQueryCount());
              }
            }
        );
        clusterRunningMap.put(stat.getRoutingGroup(), new HashMap<String, Integer>() {
              {
                put(stat.getClusterId(), stat.getRunningQueryCount());
              }
            }
        );
      } else {
        clusterQueueMap.get(stat.getRoutingGroup()).put(stat.getClusterId(),
            stat.getQueuedQueryCount());
        clusterRunningMap.get(stat.getRoutingGroup()).put(stat.getClusterId(),
                stat.getRunningQueryCount());
      }

      // Create inverse map from user -> {cluster-> count}
      if (stat.getUserQueuedCount() != null && !stat.getUserQueuedCount().isEmpty()) {
        for (Map.Entry<String, Integer> queueCount : stat.getUserQueuedCount().entrySet()) {
          Map<String, Integer> clusterQueue = userClusterQueuedCount.getOrDefault(
                  queueCount.getKey(), new HashMap<>());
          clusterQueue.put(stat.getClusterId(), queueCount.getValue());
          userClusterQueuedCount.put(queueCount.getKey(), clusterQueue);
        }
      }
    }

    routingManager.updateRoutingTable(clusterQueueMap, clusterRunningMap, userClusterQueuedCount);
  }
}
