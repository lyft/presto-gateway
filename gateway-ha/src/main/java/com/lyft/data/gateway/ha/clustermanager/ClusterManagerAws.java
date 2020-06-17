package com.lyft.data.gateway.ha.clustermanager;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.SetDesiredCapacityRequest;
import software.amazon.awssdk.services.autoscaling.model.SetDesiredCapacityResponse;

/** Author: taulakh@lyft.com Created on: 6/11/20 */
@RequiredArgsConstructor
public class ClusterManagerAws implements ClusterManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerAws.class);

  // https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/autoscaling/AutoScalingClient.html
  @NonNull private final AutoScalingClient autoScalingClient;

  @Override
  public boolean setWorkerCapacity(String clusterId, int workerCount) {
    String workerAsgName = clusterId;

    SetDesiredCapacityRequest setDesiredCapacityRequest =
        SetDesiredCapacityRequest.builder()
            .autoScalingGroupName(workerAsgName)
            .desiredCapacity(workerCount)
            .build();

    SetDesiredCapacityResponse response =
           autoScalingClient.setDesiredCapacity(setDesiredCapacityRequest);

    LOGGER.info("isSuccessful: " + response.sdkHttpResponse().isSuccessful());
    LOGGER.info("sdkHttpResponse Headers: " + response.sdkHttpResponse().headers());

    return response.sdkHttpResponse().isSuccessful();
  }
}
