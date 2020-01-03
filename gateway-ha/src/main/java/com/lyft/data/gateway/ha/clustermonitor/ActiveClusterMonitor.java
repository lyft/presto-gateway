package com.lyft.data.gateway.ha.clustermonitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
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
  private static final int BACKEND_CONNECT_TIMEOUT_SECONDS = 15;
  private static final int MONITOR_TASK_DELAY_MIN = 1;

  @Inject
  private List<PrestoClusterStatsObserver> clusterStatsObservers;
  @Inject
  private GatewayBackendManager gatewayBackendManager;

  private volatile boolean monitorActive = true;

  private ExecutorService executorService = Executors.newFixedThreadPool(10);
  private ExecutorService singleTaskExecutor = Executors.newSingleThreadExecutor();

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
              Thread.sleep(TimeUnit.MINUTES.toMillis(MONITOR_TASK_DELAY_MIN));
            } catch (Exception e) {
              log.error("Error with monitor task", e);
            }
          }
        });
  }

  private ClusterStats getPrestoClusterStats(ProxyBackendConfiguration backend) {
    ClusterStats clusterStats = new ClusterStats();
    clusterStats.setClusterId(backend.getName());

    String target = backend.getProxyTo() + "/v1/cluster";
    try {
      URL url = new URL(target);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(BACKEND_CONNECT_TIMEOUT_SECONDS));
      conn.setReadTimeout((int) TimeUnit.SECONDS.toMillis(BACKEND_CONNECT_TIMEOUT_SECONDS));
      conn.setRequestMethod(HttpMethod.GET);
      conn.connect();
      int responseCode = conn.getResponseCode();
      if (responseCode == HttpStatus.SC_OK) {
        clusterStats.setHealthy(true);
        BufferedReader reader =
            new BufferedReader(new InputStreamReader((InputStream) conn.getContent()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
          sb.append(line + "\n");
        }
        HashMap<String, Object> result = OBJECT_MAPPER.readValue(sb.toString(), HashMap.class);
        clusterStats.setNumWorkerNodes((int) result.get("activeWorkers"));
        clusterStats.setQueuedQueryCount((int) result.get("queuedQueries"));
        clusterStats.setRunningQueryCount((int) result.get("runningQueries"));
        clusterStats.setBlockedQueryCount((int) result.get("blockedQueries"));
        clusterStats.setProxyTo(backend.getProxyTo());
        clusterStats.setRoutingGroup(backend.getRoutingGroup());
      } else {
        log.warn("Received non 200 response, response code: {}", responseCode);
      }
      conn.disconnect();
    } catch (Exception e) {
      log.error("Error fetching cluster stats from [{}]", target, e);
    }
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
