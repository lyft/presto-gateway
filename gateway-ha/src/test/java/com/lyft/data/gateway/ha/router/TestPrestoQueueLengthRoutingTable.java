package com.lyft.data.gateway.ha.router;

import com.lyft.data.gateway.ha.HaGatewayTestUtils;
import com.lyft.data.gateway.ha.config.DataStoreConfiguration;
import com.lyft.data.gateway.ha.config.ProxyBackendConfiguration;
import com.lyft.data.gateway.ha.persistence.JdbcConnectionManager;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Test
public class TestPrestoQueueLengthRoutingTable {
    PrestoQueueLengthRoutingTable routingTable;
    GatewayBackendManager backendManager;
    QueryHistoryManager historyManager;
    String[] mockAdhocBackeds = {"adhoc0"};
    String mockRoutingGroup = "adhoc";
    Map<String, Map<String, Integer>> clusterQueueMap;
    final int[] QUERY_VOLUMES = {15, 50, 100, 200};
    final int NUM_BACKENDS = 5;

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        File baseDir = new File(System.getProperty("java.io.tmpdir"));
        File tempH2DbDir = new File(baseDir, "h2db-" + System.currentTimeMillis());
        tempH2DbDir.deleteOnExit();
        String jdbcUrl = "jdbc:h2:" + tempH2DbDir.getAbsolutePath();
        HaGatewayTestUtils.seedRequiredData(
                new HaGatewayTestUtils.TestConfig("", tempH2DbDir.getAbsolutePath()));
        DataStoreConfiguration db = new DataStoreConfiguration(jdbcUrl, "sa", "sa", "org.h2.Driver");
        JdbcConnectionManager connectionManager = new JdbcConnectionManager(db);
        backendManager = new HaGatewayManager(connectionManager);
        historyManager = new HaQueryHistoryManager(connectionManager) {
        };
        routingTable = new PrestoQueueLengthRoutingTable(backendManager, historyManager);
        addMockBackends(NUM_BACKENDS, 0);
    }

    private void deactiveAllBackends() {
        for (int i = 0; i < NUM_BACKENDS; i++) {
            backendManager.deactivateBackend(mockRoutingGroup + i);
        }
    }

    private void addMockBackends(int num_backends, int queueLengthDistributiveFactor) {
        String backend = null;
        clusterQueueMap = new HashMap<>();

        for (int i = 0; i < num_backends; i++) {
            backend = mockRoutingGroup + i;
            ProxyBackendConfiguration proxyBackend = new ProxyBackendConfiguration();
            proxyBackend.setActive(true);
            proxyBackend.setRoutingGroup(mockRoutingGroup);
            proxyBackend.setName(backend);
            proxyBackend.setProxyTo(backend + ".presto.lyft.com");
            backendManager.addBackend(proxyBackend);
        }
    }

    private void registerBackEnds(int num_backends, int queueLengthDistributiveFactor) {
        clusterQueueMap = new HashMap<String, Map<String, Integer>>();
        Map<String, Integer> queueLenghts = new HashMap<>();
        int mockQueueLength = 0;
        String backend;
        for (int i = 0; i < num_backends; i++) {
            backend = mockRoutingGroup + i;
            backendManager.activateBackend(mockRoutingGroup + i);
            queueLenghts.put(backend, mockQueueLength += queueLengthDistributiveFactor);
        }

        clusterQueueMap.put(mockRoutingGroup, queueLenghts);
        routingTable.updateRoutingTable(clusterQueueMap);
    }

    private Map<String, Integer> routeQueries(int num_bk, int queueDistribution, int num_requests) {
        String eligibleBackend;
        int sum = 0;
        Map<String, Integer> routingDistribution = new HashMap<String, Integer>();
        deactiveAllBackends();
        registerBackEnds(num_bk, queueDistribution);

        for (int i = 0; i < num_requests; i++) {
            eligibleBackend = routingTable.getEligibleBackEnd(mockRoutingGroup);

            if (!routingDistribution.containsKey(eligibleBackend)) {
                routingDistribution.put(eligibleBackend, 1);
            } else {
                sum = routingDistribution.get(eligibleBackend) + 1;
                routingDistribution.put(eligibleBackend, sum);
            }
        }
        return routingDistribution;
    }

    @Test
    public void testRoutingWithEvenWeightDistribution() {

        int queueDistribution = 3;

        for (int num_requests : QUERY_VOLUMES) {
            for (int num_bk = 1; num_bk <= NUM_BACKENDS; num_bk++) {

                Map<String, Integer> routingDistribution = routeQueries(num_bk, queueDistribution, num_requests);

                System.out.println("Input :" + clusterQueueMap.toString() + " Num of Requests:" + num_requests
                        + " Internal Routing table: " + routingTable.getInternalWeightedRoutingTable(mockRoutingGroup).toString()
                        + " Distribution: " + routingDistribution.toString());
                if (num_bk > 1) {

                    if (routingDistribution.containsKey(mockRoutingGroup + (num_bk - 1)))
                        assert routingDistribution.get(mockRoutingGroup + (num_bk - 1)) <= Math.ceil(num_requests / num_bk);
                    else
                        assert routingDistribution.values().stream().mapToInt(Integer::intValue).sum() == num_requests;
                } else
                    assert routingDistribution.get(mockRoutingGroup + '0') == num_requests;
            }
        }

    }

    @Test
    public void testRoutingWithSkewedWeightDistribution() {
        int queueDistribution = 30;

        for (int num_requests : QUERY_VOLUMES) {
            for (int num_bk = 1; num_bk <= NUM_BACKENDS; num_bk++) {

                Map<String, Integer> routingDistribution = routeQueries(num_bk, queueDistribution, num_requests);

                System.out.println("Input :" + clusterQueueMap.toString() + " Num of Requests:" + num_requests
                        + " Internal Routing table: " + routingTable.getInternalWeightedRoutingTable(mockRoutingGroup).toString()
                        + " Distribution: " + routingDistribution.toString());
                if (num_bk > 1) {
                    if (routingDistribution.containsKey(mockRoutingGroup + (num_bk - 1)))
                        assert routingDistribution.get(mockRoutingGroup + (num_bk - 1)) <= Math.ceil(num_requests / num_bk);
                    else
                        assert routingDistribution.values().stream().mapToInt(Integer::intValue).sum() == num_requests;
                } else
                    assert routingDistribution.get(mockRoutingGroup + '0') == num_requests;
            }
        }
    }


}
