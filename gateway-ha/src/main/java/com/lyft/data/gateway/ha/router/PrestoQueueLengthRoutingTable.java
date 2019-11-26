package com.lyft.data.gateway.ha.router;

import com.lyft.data.gateway.ha.clustermonitor.ClusterStats;
import com.lyft.data.gateway.ha.clustermonitor.PrestoClusterStatsObserver;
import com.lyft.data.gateway.ha.config.ProxyBackendConfiguration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * A Routing Manager that provides load distribution on to registered active backends based on its queue length.
 * This manager listens to updates on cluster queue length as recorded by the
 * {@link com.lyft.data.gateway.ha.clustermonitor.ActiveClusterMonitor}
 * Ideally where ever a modification is made to the the list of backends(adding, removing) this routing
 * manager should get notified & updated.
 * Currently updates are made only on heart beats from
 * {@link com.lyft.data.gateway.ha.clustermonitor.ActiveClusterMonitor} & during routing requests.
 */
@Slf4j
public class PrestoQueueLengthRoutingTable extends HaRoutingManager implements PrestoClusterStatsObserver {
    private static final Random RANDOM = new Random();
    private ConcurrentHashMap<String, Integer> routingGroupWeightSum;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> clusterQueueLengthMap;
    private Map<String, TreeMap<Integer, String>> weightedDistributionRouting;
    private final Object lockObject = new Object();
    private final int MIN_WT = 1;
    private final int MAX_WT = 100;


    public PrestoQueueLengthRoutingTable(GatewayBackendManager gatewayBackendManager, QueryHistoryManager queryHistoryManager) {
        super(gatewayBackendManager, queryHistoryManager);
        routingGroupWeightSum = new ConcurrentHashMap<String, Integer>();
        clusterQueueLengthMap = new ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>>();
        weightedDistributionRouting = new HashMap<String, TreeMap<Integer, String>>();
    }

    /**
     * Uses the queue length of a cluster to assign weights to all active clusters in a routing group.
     * The weights assigned ensure a fair distribution of routing for queries such that clusters with the least
     * queue length get assigned more queries.
     *
     * @param queueLengthMap
     */
    private void computeWeightsBasedOnQueueLength(ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> queueLengthMap) {
        synchronized (lockObject) {
            int sum, queue_sum = 0, weight, num_buckets = 1, equal_distribution = 0, smallest_queue_ln = 0, last_but_one_queue_ln = 0,
                    max_queue_ln = 0, calculated_wt_max_queue = 0;

            weightedDistributionRouting.clear();
            routingGroupWeightSum.clear();

            log.debug("Computing Weights for Queue Map " + queueLengthMap.toString());

            for (String routingGroup : queueLengthMap.keySet()) {
                sum = 0;
                TreeMap<Integer, String> weightsMap = new TreeMap<>();

                if (queueLengthMap.get(routingGroup).size() == 0) {
                    log.warn("No active clusters in routingGroup : " + routingGroup + ". Continue to process rest of routing table ");
                    continue;
                } else if (queueLengthMap.get(routingGroup).size() == 1) {
                    log.debug("Routing Group: " + routingGroup + " has only 1 active backend. ");
                    weightedDistributionRouting.put(routingGroup, new TreeMap<Integer, String>() {{
                        put(MAX_WT, queueLengthMap.get(routingGroup).keys().nextElement());
                    }});
                    routingGroupWeightSum.put(routingGroup, MAX_WT);
                    continue;
                }

                Map<String, Integer> sortedByQueueLength = queueLengthMap.get(routingGroup).entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                (e1, e2) -> e1, LinkedHashMap::new));

                num_buckets = sortedByQueueLength.size();
                queue_sum = sortedByQueueLength.values().stream().mapToInt(Integer::intValue).sum();
                equal_distribution = queue_sum / num_buckets;

                Object[] queueLengths = sortedByQueueLength.values().toArray();
                Object[] clusterNames = sortedByQueueLength.keySet().toArray();

                smallest_queue_ln = (Integer)queueLengths[0];
                max_queue_ln = (Integer)queueLengths[queueLengths.length - 1];
                last_but_one_queue_ln = smallest_queue_ln;
                if (queueLengths.length > 2) {
                    last_but_one_queue_ln = (Integer)queueLengths[queueLengths.length - 2];
                }

                /**
                 *  All wts are assigned as a fraction of max_queue_ln. Cluster with max_queue_ln should be given least
                 *  weightage. What this value should be depends on the queueing on the rest of the clusters. since the list
                 *  is sorted, the smallest_queue_ln & last_but_one_queue_ln give us the range.
                 *
                 *  In an ideal situation all clusters should have equals distribution of queries, hence that is used as
                 *  a threshold to check is a cluster queue is over provisioned or not.
                 */

                if (max_queue_ln == 0) {
                    calculated_wt_max_queue = MAX_WT;
                } else if (last_but_one_queue_ln == 0) {
                    calculated_wt_max_queue = MIN_WT;
                } else {
                    float fraction_of_last_but_one_wt = (last_but_one_queue_ln / (float) max_queue_ln) * (MAX_WT - (last_but_one_queue_ln * MAX_WT / max_queue_ln));

                    if (smallest_queue_ln == 0) {
                        if (last_but_one_queue_ln < equal_distribution) {
                            calculated_wt_max_queue = MIN_WT;
                        } else {
                            calculated_wt_max_queue = (int)fraction_of_last_but_one_wt;
                        }
                    } else if (smallest_queue_ln < equal_distribution) {
                        if (last_but_one_queue_ln <= equal_distribution) {
                            calculated_wt_max_queue = (fraction_of_last_but_one_wt / 2 > 0 ) ? (int)fraction_of_last_but_one_wt / 2 : MIN_WT;
                        } else {
                            calculated_wt_max_queue = (fraction_of_last_but_one_wt/1.5 > 0) ? (int)(fraction_of_last_but_one_wt/1.5): MIN_WT;
                        }
                    } else {
                        calculated_wt_max_queue = (int)fraction_of_last_but_one_wt;
                    }
                }

                for (int i = 0; i < num_buckets - 1; i++) {
                    // If all clusters have same queue length, assign same wt
                    weight = (max_queue_ln == (Integer)queueLengths[i]) ? calculated_wt_max_queue : MAX_WT - (((Integer)queueLengths[i] * MAX_WT) / max_queue_ln);
                    sum += weight;
                    weightsMap.put(sum, (String) clusterNames[i]);
                }

                sum += calculated_wt_max_queue;
                weightsMap.put(sum, (String)clusterNames[num_buckets - 1]);

                weightedDistributionRouting.put(routingGroup, weightsMap);
                routingGroupWeightSum.put(routingGroup, sum);
            }

            if (log.isDebugEnabled()) {
                for (String rg : weightedDistributionRouting.keySet()) {
                    log.debug(String.format("Routing Table for : {%s} is {%s}", rg, weightedDistributionRouting.get(rg).toString()));
                }
            }
        }
    }

    /**
     * Update the Routing Table only if a previously known backend has been deactivated.
     * Newly added backends are handled through {@link PrestoQueueLengthRoutingTable#updateRoutingTable(Map)}
     * updateRoutingTable}
     *
     * @param routingGroup
     * @param backends
     */
    protected void updateRoutingTable(String routingGroup, Set<String> backends) {

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
     * Update routing Table with new Queue Lengths
     *
     * @param updatedQueueLengthMap
     */
    protected void updateRoutingTable(Map<String, Map<String, Integer>> updatedQueueLengthMap) {
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
     * @param routingGroup
     * @return
     */
    public Map<String, Integer> getInternalWeightedRoutingTable(String routingGroup) {
        if (!weightedDistributionRouting.containsKey(routingGroup))
            return null;
        Map<String, Integer> routingTable = new HashMap<>();

        for (Integer wt : weightedDistributionRouting.get(routingGroup).keySet()) {
            routingTable.put(weightedDistributionRouting.get(routingGroup).get(wt), wt);
        }
        return routingTable;
    }

    /**
     * @param routingGroup
     * @return
     */
    public Map<String, Integer> getInternalClusterQueueLength(String routingGroup) {
        if (!clusterQueueLengthMap.containsKey(routingGroup))
            return null;

        return clusterQueueLengthMap.get(routingGroup);
    }

    /**
     * Looks up the closest weight to random number generated for a given routing group.
     *
     * @param routingGroup
     * @return
     */
    public String getEligibleBackEnd(String routingGroup) {
        if (routingGroupWeightSum.containsKey(routingGroup) && weightedDistributionRouting.containsKey(routingGroup)) {
            int rnd = RANDOM.nextInt(routingGroupWeightSum.get(routingGroup));
            return weightedDistributionRouting.get(routingGroup).higherEntry(rnd).getValue();
        } else
            return null;
    }


    @Override
    public void observe(List<ClusterStats> stats) {
        Map<String, Map<String, Integer>> clusterQueueMap = new HashMap<String, Map<String, Integer>>();

        for (ClusterStats stat : stats) {
            if (!clusterQueueMap.containsKey(stat.getRoutingGroup())) {
                clusterQueueMap.put(stat.getRoutingGroup(), new HashMap<String, Integer>() {{
                    put(stat.getClusterId(), stat.getQueuedQueryCount());
                }});
            } else {
                clusterQueueMap.get(stat.getRoutingGroup()).put(stat.getClusterId(), stat.getQueuedQueryCount());
            }
        }

        updateRoutingTable(clusterQueueMap);
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
