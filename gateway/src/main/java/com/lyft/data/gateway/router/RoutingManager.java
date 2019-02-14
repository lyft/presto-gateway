package com.lyft.data.gateway.router;

import com.google.common.base.Strings;
import com.lyft.data.gateway.config.ProxyBackendConfiguration;
import com.lyft.data.proxyserver.ProxyServerConfiguration;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import lombok.extern.slf4j.Slf4j;

import org.ehcache.Cache;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;

/**
 * This class performs health check, stats counts for each backend and provides a backend given
 * request object. Default implementation comes here.
 */
@Slf4j
public abstract class RoutingManager {
  private final AtomicLong requestAdhocCounter = new AtomicLong(0);
  private final AtomicLong requestScheduledCounter = new AtomicLong(0);
  private final Cache<String, String> queryIdBackendCache;
  private ExecutorService executorService = Executors.newFixedThreadPool(5);
  private GatewayBackendManager gatewayBackendManager;

  public RoutingManager(GatewayBackendManager gatewayBackendManager, String cacheDataDir) {
    this.gatewayBackendManager = gatewayBackendManager;

    PersistentCacheManager persistentCacheManager =
        CacheManagerBuilder.newCacheManagerBuilder()
            .with(CacheManagerBuilder.persistence(new File(cacheDataDir, "queryIdBackendMapping")))
            .withCache(
                "queryIdBackendPersistentCache",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(
                    String.class,
                    String.class,
                    ResourcePoolsBuilder.newResourcePoolsBuilder()
                        .heap(1000, EntryUnit.ENTRIES)
                        .offheap(100, MemoryUnit.MB)
                        .disk(1, MemoryUnit.GB, true)))
            .build(true);
    this.queryIdBackendCache =
        persistentCacheManager.getCache(
            "queryIdBackendPersistentCache", String.class, String.class);
  }

  public void setBackendForQueryId(String queryId, String backend) {
    queryIdBackendCache.put(queryId, backend);
  }

  /**
   * Performs routing to an adhoc backend.
   *
   * @return
   */
  public String provideAdhocBackendForThisRequest() {
    List<ProxyBackendConfiguration> backends = this.gatewayBackendManager.getActiveAdhocBackends();
    int backendId = (int) (requestAdhocCounter.incrementAndGet() % backends.size());
    if (requestAdhocCounter.get() >= Long.MAX_VALUE - 1) {
      requestAdhocCounter.set(0);
    }
    return backends.get(backendId).getProxyTo();
  }

  /**
   * Performs routing to a scheduled backend. This falls back to an adhoc backend, if no scheduled
   * backend is found.
   *
   * @return
   */
  public String provideScheduledBackendForThisRequest() {
    List<ProxyBackendConfiguration> backends =
        this.gatewayBackendManager.getActiveScheduledBackends();
    if (backends.isEmpty()) {
      return provideAdhocBackendForThisRequest();
    }
    int backendId = (int) (requestScheduledCounter.incrementAndGet() % backends.size());
    if (requestScheduledCounter.get() >= Long.MAX_VALUE - 1) {
      requestScheduledCounter.set(0);
    }
    return backends.get(backendId).getProxyTo();
  }

  /**
   * Performs cache look up, if a backend not found, it checks with all backends and tries to find
   * out which backend has info about given query id.
   *
   * @param queryId
   * @return
   */
  public String findBackendForQueryId(String queryId) {
    String backendAddress = queryIdBackendCache.get(queryId);
    if (Strings.isNullOrEmpty(backendAddress)) {
      log.error("Could not find mapping for query id {}", queryId);
      // for now resort to defaulting to first backend
      backendAddress = findBackendForUnknownQueryId(queryId);
    }
    return backendAddress;
  }

  /**
   * This tries to find out which backend may have info about given query id. If not found returns
   * the first healthy backend.
   *
   * @param queryId
   * @return
   */
  private String findBackendForUnknownQueryId(String queryId) {
    List<ProxyBackendConfiguration> backends = this.gatewayBackendManager.getAllBackends();

    Map<String, Future<Integer>> responseCodes = new HashMap<>();
    try {
      for (ProxyServerConfiguration backend : backends) {
        String target = backend.getProxyTo() + "/v1/query/" + queryId;

        Future<Integer> call =
            executorService.submit(
                () -> {
                  URL url = new URL(target);
                  HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                  conn.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(5));
                  conn.setReadTimeout((int) TimeUnit.SECONDS.toMillis(5));
                  conn.setRequestMethod("HEAD");
                  return conn.getResponseCode();
                });
        responseCodes.put(backend.getProxyTo(), call);
      }
      for (Map.Entry<String, Future<Integer>> entry : responseCodes.entrySet()) {
        if (entry.getValue().isDone()) {
          int responseCode = entry.getValue().get();
          if (responseCode == 200) {
            log.info("Found query [{}] on backend [{}]", queryId, entry.getKey());
            setBackendForQueryId(queryId, entry.getKey());
            return entry.getKey();
          }
        }
      }
    } catch (Exception e) {
      log.warn("Query id [{}] not found", queryId);
    }
    return backends.get(0).getProxyTo();
  }
}
