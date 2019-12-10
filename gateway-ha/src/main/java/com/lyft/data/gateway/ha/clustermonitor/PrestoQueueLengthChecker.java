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

    for (ClusterStats stat : stats) {
      if (!clusterQueueMap.containsKey(stat.getRoutingGroup())) {
        clusterQueueMap.put(stat.getRoutingGroup(), new HashMap<String, Integer>() {
              {
                put(stat.getClusterId(), stat.getQueuedQueryCount());
              }
            }
        );
      } else {
        clusterQueueMap.get(stat.getRoutingGroup()).put(stat.getClusterId(),
            stat.getQueuedQueryCount());
      }
    }

    routingManager.updateRoutingTable(clusterQueueMap);
  }
}
