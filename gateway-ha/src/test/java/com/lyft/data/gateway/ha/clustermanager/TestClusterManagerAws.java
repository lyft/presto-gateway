package com.lyft.data.gateway.ha.clustermanager;

import org.testng.Assert;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;

/** Author: taulakh@lyft.com Created on: 6/11/20 */
public class TestClusterManagerAws {

  private final ClusterManager clusterManager =
      new ClusterManagerAws(AutoScalingClient.builder().build());

  @Test
  public void testSetWorkerCapacity() {
    String clusterId = "prestoinfranolimitworker-staging-iad";
    int workerCount = 5;

    boolean resp = clusterManager.setWorkerCapacity(clusterId, workerCount);
    Assert.assertTrue(resp);
  }
}
