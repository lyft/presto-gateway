package com.lyft.data.gateway.ha.clustermonitor;

import java.util.List;

public interface PrestoClusterStatsObserver {

  void observe(List<ClusterStats> stats);
}
