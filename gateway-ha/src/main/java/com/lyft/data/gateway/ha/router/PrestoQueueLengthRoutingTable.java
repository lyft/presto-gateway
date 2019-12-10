package com.lyft.data.gateway.ha.router;

import com.lyft.data.gateway.ha.config.ProxyBackendConfiguration;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * A Routing Manager that provides load distribution on to registered active backends based on
 * its queue length. This manager listens to updates on cluster queue length as recorded by the
 * {@link com.lyft.data.gateway.ha.clustermonitor.ActiveClusterMonitor}
 * Ideally where ever a modification is made to the the list of backends(adding, removing) this
 * routing manager should get notified & updated.
 * Currently updates are made only on heart beats from
 * {@link com.lyft.data.gateway.ha.clustermonitor.ActiveClusterMonitor} & during routing requests.
 */
@Slf4j
public class PrestoQueueLengthRoutingTable extends HaRoutingManager {
  private static final Random RANDOM = new Random();
  private final Object lockObject = new Object();
  private static final int MIN_WT = 1;
  private static final int MAX_WT = 100;
  private ConcurrentHashMap<String, Integer> routingGroupWeightSum;
  private ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> clusterQueueLengthMap;
  private Map<String, TreeMap<Integer, String>> weightedDistributionRouting;

  /**
   * A Routing Manager that distributes queries according to assigned weights based on
   * Presto cluster queue length.
   *
   * @param gatewayBackendManager
   * @param queryHistoryManager
   */
  public PrestoQueueLengthRoutingTable(GatewayBackendManager gatewayBackendManager,
                                       QueryHistoryManager queryHistoryManager) {
    super(gatewayBackendManager, queryHistoryManager);
    routingGroupWeightSum = new ConcurrentHashMap<String, Integer>();
    clusterQueueLengthMap = new ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>>();
    weightedDistributionRouting = new HashMap<String, TreeMap<Integer, String>>();
  }


  /**
   * Uses the queue length of a cluster to assign weights to all active clusters in a routing group.
   * The weights assigned ensure a fair distribution of routing for queries such that clusters with
   * the least queue length get assigned more queries.
   *
   * @param queueLengthMap
   */
  private void computeWeightsBasedOnQueueLength(ConcurrentHashMap<String,
      ConcurrentHashMap<String, Integer>> queueLengthMap) {
    synchronized (lockObject) {
      int sum = 0;
      int queueSum = 0;
      int weight;
      int numBuckets = 1;
      int equalDistribution = 0;
      int smallestQueueLn = 0;
      int lastButOneQueueLn = 0;
      int maxQueueLn = 0;
      int calculatedWtMaxQueue = 0;

      weightedDistributionRouting.clear();
      routingGroupWeightSum.clear();

      log.debug("Computing Weights for Queue Map " + queueLengthMap.toString());

      for (String routingGroup : queueLengthMap.keySet()) {
        sum = 0;
        TreeMap<Integer, String> weightsMap = new TreeMap<>();

        if (queueLengthMap.get(routingGroup).size() == 0) {
          log.warn("No active clusters in routingGroup : " + routingGroup + ". Continue to "
              + "process rest of routing table ");
          continue;
        } else if (queueLengthMap.get(routingGroup).size() == 1) {
          log.debug("Routing Group: " + routingGroup + " has only 1 active backend. ");
          weightedDistributionRouting.put(routingGroup, new TreeMap<Integer, String>() {
                {
                  put(MAX_WT, queueLengthMap.get(routingGroup).keys().nextElement());
                }
              }
          );
          routingGroupWeightSum.put(routingGroup, MAX_WT);
          continue;
        }

        Map<String, Integer> sortedByQueueLength = queueLengthMap.get(routingGroup).entrySet()
            .stream().sorted(Comparator.comparing(Map.Entry::getValue))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                (e1, e2) -> e1, LinkedHashMap::new));

        numBuckets = sortedByQueueLength.size();
        queueSum = sortedByQueueLength.values().stream().mapToInt(Integer::intValue).sum();
        equalDistribution = queueSum / numBuckets;

        Object[] queueLengths = sortedByQueueLength.values().toArray();
        Object[] clusterNames = sortedByQueueLength.keySet().toArray();

        smallestQueueLn = (Integer) queueLengths[0];
        maxQueueLn = (Integer) queueLengths[queueLengths.length - 1];
        lastButOneQueueLn = smallestQueueLn;
        if (queueLengths.length > 2) {
          lastButOneQueueLn = (Integer) queueLengths[queueLengths.length - 2];
        }

        /**
         *  All wts are assigned as a fraction of maxQueueLn. Cluster with maxQueueLn should be
         *  given least weightage. What this value should be depends on the queueing on the rest of
         *  the clusters. since the list is sorted, the smallestQueueLn & lastButOneQueueLn give us
         *  the range. In an ideal situation all clusters should have equals distribution of
         *  queries, hence that is used as a threshold to check is a cluster queue is over
         *  provisioned or not.
         */

        if (maxQueueLn == 0) {
          calculatedWtMaxQueue = MAX_WT;
        } else if (lastButOneQueueLn == 0) {
          calculatedWtMaxQueue = MIN_WT;
        } else {
          float fractionOfLastWt =
              (lastButOneQueueLn / (float) maxQueueLn)
                  * (MAX_WT - (lastButOneQueueLn * MAX_WT / maxQueueLn));

          if (smallestQueueLn == 0) {
            if (lastButOneQueueLn < equalDistribution) {
              calculatedWtMaxQueue = MIN_WT;
            } else {
              calculatedWtMaxQueue = (int) fractionOfLastWt;
            }
          } else if (smallestQueueLn < equalDistribution) {
            if (lastButOneQueueLn <= equalDistribution) {
              calculatedWtMaxQueue = (fractionOfLastWt / 2 > 0)
                  ? (int) fractionOfLastWt / 2 : MIN_WT;
            } else {
              calculatedWtMaxQueue = (fractionOfLastWt / 1.5 > 0)
                  ? (int) (fractionOfLastWt / 1.5) : MIN_WT;
            }
          } else {
            calculatedWtMaxQueue = (int) fractionOfLastWt;
          }
        }

        for (int i = 0; i < numBuckets - 1; i++) {
          // If all clusters have same queue length, assign same wt
          weight = (maxQueueLn == (Integer) queueLengths[i]) ? calculatedWtMaxQueue :
              MAX_WT - (((Integer) queueLengths[i] * MAX_WT) / maxQueueLn);
          sum += weight;
          weightsMap.put(sum, (String) clusterNames[i]);
        }

        sum += calculatedWtMaxQueue;
        weightsMap.put(sum, (String) clusterNames[numBuckets - 1]);

        weightedDistributionRouting.put(routingGroup, weightsMap);
        routingGroupWeightSum.put(routingGroup, sum);
      }

      if (log.isDebugEnabled()) {
        for (String rg : weightedDistributionRouting.keySet()) {
          log.debug(String.format("Routing Table for : {%s} is {%s}", rg,
              weightedDistributionRouting.get(rg).toString()));
        }
      }
    }
  }

  /**
   * Update the Routing Table only if a previously known backend has been deactivated.
   * Newly added backends are handled through
   * {@link PrestoQueueLengthRoutingTable#updateRoutingTable(Map)}
   * updateRoutingTable}
   *
   * @param routingGroup
   * @param backends
   */
  public void updateRoutingTable(String routingGroup, Set<String> backends) {

    synchronized (lockObject) {
      if (clusterQueueLengthMap.containsKey(routingGroup)) {
        Collection<String> knownBackends = new HashSet<String>();
        knownBackends.addAll(clusterQueueLengthMap.get(routingGroup).keySet());

        if (backends.containsAll(knownBackends)) {
          return;
        } else {
          if (knownBackends.removeAll(backends)) {
            for (String inactiveBackend : knownBackends) {
              clusterQueueLengthMap.get(routingGroup).remove(inactiveBackend);
            }
          }
        }
      }

      computeWeightsBasedOnQueueLength(clusterQueueLengthMap);
    }
  }

  /**
   * Update routing Table with new Queue Lengths.
   *
   * @param updatedQueueLengthMap
   */
  public void updateRoutingTable(Map<String, Map<String, Integer>> updatedQueueLengthMap) {
    synchronized (lockObject) {
      clusterQueueLengthMap.clear();

      for (String grp : updatedQueueLengthMap.keySet()) {
        ConcurrentHashMap<String, Integer> queueMap = new ConcurrentHashMap<>();
        queueMap.putAll(updatedQueueLengthMap.get(grp));
        clusterQueueLengthMap.put(grp, queueMap);
      }

      computeWeightsBasedOnQueueLength(clusterQueueLengthMap);
    }

  }

  /**
   * A convenience method to peak into the weights used by the routing Manager.
   *
   * @param routingGroup
   * @return
   */
  public Map<String, Integer> getInternalWeightedRoutingTable(String routingGroup) {
    if (!weightedDistributionRouting.containsKey(routingGroup)) {
      return null;
    }
    Map<String, Integer> routingTable = new HashMap<>();

    for (Integer wt : weightedDistributionRouting.get(routingGroup).keySet()) {
      routingTable.put(weightedDistributionRouting.get(routingGroup).get(wt), wt);
    }
    return routingTable;
  }

  /**
   * A convienience method to get a peak into the state of the routing manager.
   *
   * @param routingGroup
   * @return
   */
  public Map<String, Integer> getInternalClusterQueueLength(String routingGroup) {
    if (!clusterQueueLengthMap.containsKey(routingGroup)) {
      return null;
    }

    return clusterQueueLengthMap.get(routingGroup);
  }

  /**
   * Looks up the closest weight to random number generated for a given routing group.
   *
   * @param routingGroup
   * @return
   */
  public String getEligibleBackEnd(String routingGroup) {
    if (routingGroupWeightSum.containsKey(routingGroup)
        && weightedDistributionRouting.containsKey(routingGroup)) {
      int rnd = RANDOM.nextInt(routingGroupWeightSum.get(routingGroup));
      return weightedDistributionRouting.get(routingGroup).higherEntry(rnd).getValue();
    } else {
      return null;
    }
  }

  /**
   * Performs routing to a given cluster group. This falls back to an adhoc backend, if no scheduled
   * backend is found.
   *
   * @return
   */
  @Override
  public String provideBackendForRoutingGroup(String routingGroup) {
    List<ProxyBackendConfiguration> backends =
        getGatewayBackendManager().getActiveBackends(routingGroup);

    if (backends.isEmpty()) {
      return provideAdhocBackend();
    }
    Map<String, String> proxyMap = new HashMap<>();
    for (ProxyBackendConfiguration backend : backends) {
      proxyMap.put(backend.getName(), backend.getProxyTo());
    }
    updateRoutingTable(routingGroup, proxyMap.keySet());

    String clusterId = getEligibleBackEnd(routingGroup);
    if (clusterId != null) {
      return proxyMap.get(clusterId);
    } else {
      int backendId = Math.abs(RANDOM.nextInt()) % backends.size();
      return backends.get(backendId).getProxyTo();
    }
  }


  /**
   * Performs routing to an adhoc backend based compute weights base don cluster queue depth.
   *
   * <p>d.
   *
   * @return
   */
  public String provideAdhocBackend() {
    Map<String, String> proxyMap = new HashMap<>();
    List<ProxyBackendConfiguration> backends = getGatewayBackendManager().getActiveAdhocBackends();
    if (backends.size() == 0) {
      throw new IllegalStateException("Number of active backends found zero");
    }

    for (ProxyBackendConfiguration backend : backends) {
      proxyMap.put(backend.getName(), backend.getProxyTo());
    }

    updateRoutingTable("adhoc", proxyMap.keySet());

    String clusterId = getEligibleBackEnd("adhoc");
    if (clusterId != null) {
      return proxyMap.get(clusterId);
    } else {
      int backendId = Math.abs(RANDOM.nextInt()) % backends.size();
      return backends.get(backendId).getProxyTo();
    }
  }

}
