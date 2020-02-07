package com.lyft.data.gateway.ha.clustermonitor;

import com.lyft.data.gateway.ha.notifier.Notifier;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FaultTolerantHealthChecker extends HealthChecker {

    public static final int DEFAULT_CONSECUTIVE_FAILURE_ALERT_COUNT = 5;
    private final Map<String, Integer> clusterHealthFailureCount;
    private final Map<String, Integer> clusterMaxQueryCountFailureCount;
    private final Map<String, Integer> clusterNoWorkerFailureCount;

    public FaultTolerantHealthChecker(Notifier notifier) {
        super(notifier);
        clusterHealthFailureCount = new ConcurrentHashMap<>();
        clusterMaxQueryCountFailureCount = new ConcurrentHashMap<>();
        clusterNoWorkerFailureCount = new ConcurrentHashMap<>();
    }

    @Override
    public void observe(List<ClusterStats> clustersStats) {
        for (ClusterStats clusterStats : clustersStats) {
            if (!clusterStats.isHealthy()) {
                Integer currentFailureCount = clusterHealthFailureCount.getOrDefault(clusterStats.getClusterId(), 0);
                currentFailureCount += 1;
                if (currentFailureCount >= DEFAULT_CONSECUTIVE_FAILURE_ALERT_COUNT) {
                    notifyUnhealthyCluster(clusterStats);
                }
                clusterHealthFailureCount.put(clusterStats.getClusterId(), currentFailureCount);
            } else {
                //reset the count when the cluster is healthy
                clusterHealthFailureCount.put(clusterStats.getClusterId(), 0);

                if (clusterStats.getQueuedQueryCount() > MAX_THRESHOLD_QUEUED_QUERY_COUNT) {
                    Integer currentFailureCount = clusterMaxQueryCountFailureCount.getOrDefault(clusterStats.getClusterId(), 0);
                    currentFailureCount += 1;
                    if (currentFailureCount >= DEFAULT_CONSECUTIVE_FAILURE_ALERT_COUNT) {
                        notifyForTooManyQueuedQueries(clusterStats);
                    }
                    clusterMaxQueryCountFailureCount.put(clusterStats.getClusterId(), currentFailureCount);
                } else {
                    clusterMaxQueryCountFailureCount.put(clusterStats.getClusterId(), 0);
                }
                if (clusterStats.getNumWorkerNodes() < 1) {
                    Integer currentFailureCount = clusterNoWorkerFailureCount.getOrDefault(clusterStats.getClusterId(), 0);
                    currentFailureCount += 1;
                    if (currentFailureCount >= DEFAULT_CONSECUTIVE_FAILURE_ALERT_COUNT) {
                        notifyForNoWorkers(clusterStats);
                    }
                    clusterNoWorkerFailureCount.put(clusterStats.getClusterId(), currentFailureCount);
                } else {
                    clusterNoWorkerFailureCount.put(clusterStats.getClusterId(), 0);
                }
            }
        }
    }
}
