package com.lyft.data.gateway.ha.clustermonitor;

import static com.lyft.data.gateway.ha.handler.QueryIdCachingProxyHandler.UI_API_QUEUED_LIST_PATH;
import static com.lyft.data.gateway.ha.handler.QueryIdCachingProxyHandler.UI_API_STATS_PATH;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.lyft.data.gateway.ha.config.MonitorConfiguration;
import com.lyft.data.gateway.ha.config.ProxyBackendConfiguration;
import com.lyft.data.gateway.ha.router.GatewayBackendManager;
import io.dropwizard.lifecycle.Managed;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.HttpMethod;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;

@Slf4j
public class ActiveClusterMonitor implements Managed {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  public static final int BACKEND_CONNECT_TIMEOUT_SECONDS = 15;
  public static final int MONITOR_TASK_DELAY_MIN = 1;
  public static final int DEFAULT_THREAD_POOL_SIZE = 20;

  private static final String SESSION_USER = "sessionUser";

  private final List<PrestoClusterStatsObserver> clusterStatsObservers;
  private final GatewayBackendManager gatewayBackendManager;
  private final int connectionTimeout;
  private final int taskDelayMin;

  private volatile boolean monitorActive = true;

  private ExecutorService executorService = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE);
  private ExecutorService singleTaskExecutor = Executors.newSingleThreadExecutor();

  @Inject
  public ActiveClusterMonitor(
      List<PrestoClusterStatsObserver> clusterStatsObservers,
      GatewayBackendManager gatewayBackendManager,
      MonitorConfiguration monitorConfiguration) {
    this.clusterStatsObservers = clusterStatsObservers;
    this.gatewayBackendManager = gatewayBackendManager;
    this.connectionTimeout = monitorConfiguration.getConnectionTimeout();
    this.taskDelayMin = monitorConfiguration.getTaskDelayMin();
    log.info("Running cluster monitor with connection timeout of {} and task delay of {}",
        connectionTimeout, taskDelayMin);
  }

  /**
   * Run an app that queries all active presto clusters for stats.
   */
  public void start() {
    singleTaskExecutor.submit(
        () -> {
          while (monitorActive) {
            try {
              List<ProxyBackendConfiguration> activeClusters =
                  gatewayBackendManager.getAllActiveBackends();
              List<Future<ClusterStats>> futures = new ArrayList<>();
              for (ProxyBackendConfiguration backend : activeClusters) {
                Future<ClusterStats> call =
                    executorService.submit(() -> getPrestoClusterStats(backend));
                futures.add(call);
              }
              List<ClusterStats> stats = new ArrayList<>();
              for (Future<ClusterStats> clusterStatsFuture : futures) {
                ClusterStats clusterStats = clusterStatsFuture.get();
                stats.add(clusterStats);
              }

              if (clusterStatsObservers != null) {
                for (PrestoClusterStatsObserver observer : clusterStatsObservers) {
                  observer.observe(stats);
                }
              }

            } catch (Exception e) {
              log.error("Error performing backend monitor tasks", e);
            }
            try {
              Thread.sleep(TimeUnit.MINUTES.toMillis(taskDelayMin));
            } catch (Exception e) {
              log.error("Error with monitor task", e);
            }
          }
        });
  }

  private String queryCluster(String target) {
    HttpURLConnection conn = null;
    try {
      URL url = new URL(target);
      conn = (HttpURLConnection) url.openConnection();
      conn.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(connectionTimeout));
      conn.setReadTimeout((int) TimeUnit.SECONDS.toMillis(connectionTimeout));
      conn.setRequestMethod(HttpMethod.GET);
      conn.connect();
      int responseCode = conn.getResponseCode();
      if (responseCode == HttpStatus.SC_OK) {
        BufferedReader reader =
                new BufferedReader(new InputStreamReader((InputStream) conn.getContent()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
          sb.append(line + "\n");
        }

        return sb.toString();
      } else {
        log.warn("Received non 200 response, response code: {}", responseCode);
      }
    } catch (Exception e) {
      log.error("Error fetching cluster stats from [{}]", target, e);
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
    return null;
  }

  private ClusterStats getPrestoClusterStats(ProxyBackendConfiguration backend) {
    ClusterStats clusterStats = new ClusterStats();
    clusterStats.setClusterId(backend.getName());

    // Fetch Cluster level Stats.
    String target = backend.getProxyTo() + UI_API_STATS_PATH;
    String response = queryCluster(target);
    if (Strings.isNullOrEmpty(response)) {
      log.error("Received null/empty response for {}", target);
      return  clusterStats;
    }
    clusterStats.setHealthy(true);
    try {
      HashMap<String, Object> result = null;
      result = OBJECT_MAPPER.readValue(response, HashMap.class);

      clusterStats.setNumWorkerNodes((int) result.get("activeWorkers"));
      clusterStats.setQueuedQueryCount((int) result.get("queuedQueries"));
      clusterStats.setRunningQueryCount((int) result.get("runningQueries"));
      clusterStats.setBlockedQueryCount((int) result.get("blockedQueries"));
      clusterStats.setProxyTo(backend.getProxyTo());
      clusterStats.setExternalUrl(backend.getExternalUrl());
      clusterStats.setRoutingGroup(backend.getRoutingGroup());

    } catch (Exception e) {
      log.error("Error parsing cluster stats from [{}]", response, e);
    }

    // Fetch User Level Stats.
    Map<String, Integer> clusterUserStats = new HashMap<>();
    target = backend.getProxyTo() + UI_API_QUEUED_LIST_PATH;
    response = queryCluster(target);
    if (Strings.isNullOrEmpty(response)) {
      log.error("Received null/empty response for {}", target);
      return clusterStats;
    }
    try {
      List<Map<String, Object>> queries = OBJECT_MAPPER.readValue(response,
            new TypeReference<List<Map<String, Object>>>(){});

      for (Map<String, Object> q : queries) {
        String user = (String) q.get(SESSION_USER);
        clusterUserStats.put(user, clusterUserStats.getOrDefault(user, 0) + 1);
      }
    } catch (Exception e) {
      log.error("Error parsing cluster user stats: {}", e);
    }
    clusterStats.setUserQueuedCount(clusterUserStats);

    return clusterStats;
  }

  /**
   * Shut down the app.
   */
  public void stop() {
    this.monitorActive = false;
    this.executorService.shutdown();
    this.singleTaskExecutor.shutdown();
  }

}
