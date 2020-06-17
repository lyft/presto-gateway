package com.lyft.data.gateway.ha.clustermanager;

public interface ClusterManager {

  public boolean setWorkerCapacity(String clusterId, int workerCount);

}
