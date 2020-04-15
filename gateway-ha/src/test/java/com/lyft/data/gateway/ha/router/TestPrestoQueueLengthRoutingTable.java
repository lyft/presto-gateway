package com.lyft.data.gateway.ha.router;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.lyft.data.gateway.ha.HaGatewayTestUtils;
import com.lyft.data.gateway.ha.config.DataStoreConfiguration;
import com.lyft.data.gateway.ha.config.ProxyBackendConfiguration;
import com.lyft.data.gateway.ha.persistence.JdbcConnectionManager;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test
public class TestPrestoQueueLengthRoutingTable {
  static final int[] QUERY_VOLUMES = {15, 50, 100, 200};
  static final int NUM_BACKENDS = 5;
  PrestoQueueLengthRoutingTable routingTable;
  GatewayBackendManager backendManager;
  QueryHistoryManager historyManager;
  String[] mockRoutingGroups = {"adhoc", "scheduled"};
  String mockRoutingGroup = "adhoc";
  Map<String, Map<String, Integer>> clusterQueueMap;

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

    for (String grp : mockRoutingGroups) {
      addMockBackends(grp, NUM_BACKENDS, 0);
    }
  }

  private void deactiveAllBackends() {
    for (int i = 0; i < NUM_BACKENDS; i++) {
      backendManager.deactivateBackend(mockRoutingGroup + i);
    }
    clusterQueueMap = new HashMap<>();
  }

  private void addMockBackends(String groupName, int numBackends,
                               int queueLengthDistributiveFactor) {
    String backend = null;

    for (int i = 0; i < numBackends; i++) {
      backend = groupName + i;
      ProxyBackendConfiguration proxyBackend = new ProxyBackendConfiguration();
      proxyBackend.setActive(true);
      proxyBackend.setRoutingGroup(groupName);
      proxyBackend.setName(backend);
      proxyBackend.setProxyTo(backend + ".presto.lyft.com");
      proxyBackend.setExternalUrl("presto.lyft.com");
      backendManager.addBackend(proxyBackend);
    }
  }

  private void registerBackEndsWithRandomQueueLength(String groupName, int numBackends) {
    int mockQueueLength = 0;
    String backend;
    int maxLength = 200;
    Random generator = new Random();

    Map<String, Integer> queueLenghts = new HashMap<>();

    for (int i = 0; i < numBackends; i++) {
      backend = groupName + i;
      backendManager.activateBackend(backend);
      queueLenghts.put(backend, generator.nextInt(maxLength));
    }

    clusterQueueMap.put(groupName, queueLenghts);
    routingTable.updateRoutingTable(clusterQueueMap);
  }

  private void registerBackEndsWithRandomQueueLengths(String groupName, int numBackends) {
    int mockQueueLength = 0;
    String backend;
    Random rand = new Random();
    Map<String, Integer> queueLenghts = new HashMap<>();

    for (int i = 0; i < numBackends; i++) {
      backend = groupName + i;
      backendManager.activateBackend(backend);
      queueLenghts.put(backend, mockQueueLength += rand.nextInt(100));
    }

    clusterQueueMap.put(groupName, queueLenghts);
    routingTable.updateRoutingTable(clusterQueueMap);
  }

  private void registerBackEnds(String groupName, int numBackends,
                                int queueLengthDistributiveFactor) {
    int mockQueueLength = 0;
    String backend;

    Map<String, Integer> queueLenghts = new HashMap<>();

    for (int i = 0; i < numBackends; i++) {
      backend = groupName + i;
      backendManager.activateBackend(backend);
      queueLenghts.put(backend, mockQueueLength += queueLengthDistributiveFactor);
    }

    clusterQueueMap.put(groupName, queueLenghts);
    routingTable.updateRoutingTable(clusterQueueMap);
  }

  private void resetBackends(String groupName, int numBk, int queueDistribution) {
    deactiveAllBackends();
    registerBackEnds(groupName, numBk, queueDistribution);
  }

  private Map<String, Integer> routeQueries(String groupName, int numRequests) {
    String eligibleBackend;
    int sum = 0;
    Map<String, Integer> routingDistribution = new HashMap<String, Integer>();


    for (int i = 0; i < numRequests; i++) {
      eligibleBackend = routingTable.getEligibleBackEnd(groupName);

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

    for (int numRequests : QUERY_VOLUMES) {
      for (int numBk = 1; numBk <= NUM_BACKENDS; numBk++) {

        resetBackends(mockRoutingGroup, numBk, queueDistribution);
        Map<String, Integer> routingDistribution = routeQueries(mockRoutingGroup, numRequests);

        // Useful for debugging
        //System.out.println("Input :" + clusterQueueMap.toString() + " Num of Requests:"
        //    + numRequests
        //    + " Internal Routing table: "
        //    + routingTable.getInternalWeightedRoutingTable(mockRoutingGroup).toString()
        //    + " Distribution: " + routingDistribution.toString());
        if (numBk > 1) {
          if (routingDistribution.containsKey(mockRoutingGroup + (numBk - 1))) {
            assert routingDistribution.get(mockRoutingGroup + (numBk - 1))
                <= Math.ceil(numRequests / numBk);
          } else {
            assert routingDistribution.values().stream().mapToInt(Integer::intValue).sum()
                == numRequests;
          }
        } else {
          assert routingDistribution.get(mockRoutingGroup + '0') == numRequests;
        }
      }
    }

  }

  @Test
  public void testRoutingWithSkewedWeightDistribution() {
    for (int numRequests : QUERY_VOLUMES) {
      for (int numBk = 1; numBk <= NUM_BACKENDS; numBk++) {

        deactiveAllBackends();
        registerBackEndsWithRandomQueueLengths(mockRoutingGroup, numBk);

        Map<String, Integer> routingDistribution = routeQueries(mockRoutingGroup, numRequests);

        // Useful Debugging Info
        //System.out.println("Input :" + clusterQueueMap.toString() + " Num of Requests:"
        //    + numRequests
        //    + " Internal Routing table: "
        //    + routingTable.getInternalWeightedRoutingTable(mockRoutingGroup).toString()
        //    + " Distribution: " + routingDistribution.toString());
        if (numBk > 2 && routingDistribution.containsKey(mockRoutingGroup + (numBk - 1))) {
          assert routingDistribution.get(mockRoutingGroup + (numBk - 1))
              <= Math.ceil(numRequests / numBk);
        } else  {
          assert routingDistribution.values().stream().mapToInt(Integer::intValue).sum()
              == numRequests;
        }


      }
    }
  }

  @Test
  public void testRoutingWithEqualWeightDistribution() {
    int queueDistribution = 0;

    for (int numRequests : QUERY_VOLUMES) {
      for (int numBk = 1; numBk <= NUM_BACKENDS; numBk++) {

        resetBackends(mockRoutingGroup, numBk, queueDistribution);
        Map<String, Integer> routingDistribution = routeQueries(mockRoutingGroup, numRequests);

        //Useful Debugging Info
        //System.out.println("Input :" + clusterQueueMap.toString() + " Num of Requests:" +
        //numRequests
        //+ " Internal Routing table: " + routingTable.getInternalWeightedRoutingTable
        //(mockRoutingGroup).toString()
        //+ " Distribution: " + routingDistribution.toString());

        if (numBk > 1) {
          // With equal weights, the algorithm randomly chooses from the list. Check that the
          // distribution spans atleast half of the routing group.
          assert routingDistribution.size() >= clusterQueueMap.get(mockRoutingGroup).size() / 2;
        } else {
          assert routingDistribution.get(mockRoutingGroup + '0') == numRequests;
        }
      }
    }
  }

  @Test
  public void testRoutingWithMultipleGroups() {
    int queueDistribution = 10;
    int numRequests = 15;
    int numBk = 3;

    for (String grp : mockRoutingGroups) {
      resetBackends(grp, numBk, queueDistribution);
      Map<String, Integer> routingDistribution = routeQueries(grp, numRequests);

      // Useful for debugging
      //System.out.println("Input :" + clusterQueueMap.toString() + " Num of Requests:" +
      //numRequests
      //+ " Internal Routing table: " + routingTable.getInternalWeightedRoutingTable
      //(grp).toString()
      //+ " Distribution: " + routingDistribution.toString());
      if (numBk > 1) {
        if (routingDistribution.containsKey(grp + (numBk - 1))) {
          assert routingDistribution.get(grp + (numBk - 1))
              <= Math.ceil(numRequests / numBk);
        } else {
          assert routingDistribution.values().stream().mapToInt(Integer::intValue).sum()
              == numRequests;
        }
      } else {
        assert routingDistribution.get(grp + '0') == numRequests;
      }
    }
  }

  @Test
  public void testActiveClusterMonitorUpdateAndRouting() throws InterruptedException {
    int numRequests = 10;
    int numBatches = 10;
    int sum = 0;
    int numBk = 3;

    AtomicBoolean globalToggle = new AtomicBoolean(true);
    Map<String, Integer> routingDistribution = new HashMap<>();
    Map<String, Integer> totalDistribution = new HashMap<>();

    ScheduledExecutorService scheduler =
        Executors.newScheduledThreadPool(1);

    final Runnable activeClusterMonitor = () -> {
      Map<String, Integer> queueLenghts = new HashMap<>();
      String backend;
      int queueLen = 0;
      for (int i = 0; i < numBk; i++) {
        backend = mockRoutingGroup + i;
        if (globalToggle.get()) {
          queueLen = (i < Math.ceil((float) numBk / 2)) ? 0 : 100;
        } else {
          queueLen = (i < Math.floor((float) numBk / 2)) ? 100 : 75;
        }

        queueLenghts.put(backend, queueLen);
      }
      globalToggle.set(!globalToggle.get());
      clusterQueueMap.put(mockRoutingGroup, queueLenghts);
      routingTable.updateRoutingTable(clusterQueueMap);
    };

    resetBackends(mockRoutingGroup, numBk, 0);
    scheduler.scheduleAtFixedRate(activeClusterMonitor, 0, 1, SECONDS);


    for (int batch = 0; batch < numBatches; batch++) {
      routingDistribution = routeQueries(mockRoutingGroup, numRequests);
      if (batch == 0) {
        totalDistribution.putAll(routingDistribution);
      } else {
        for (String key : routingDistribution.keySet()) {
          sum = totalDistribution.get(key) + routingDistribution.get(key);
          totalDistribution.put(key, sum);
        }
      }
      // Some random amount of sleep time
      Thread.sleep(270);
    }

    System.out.println("Total Requests :" + numBatches * numRequests
        + " distribution :" + totalDistribution.toString());
    assert totalDistribution.get(mockRoutingGroup + (numBk - 1))
        <= (numBatches * numRequests / numBk);
    scheduler.shutdown();
  }

}


