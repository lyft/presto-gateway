package com.lyft.data.gateway.router.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import com.lyft.data.gateway.config.ProxyBackendConfiguration;
import com.lyft.data.gateway.handler.QueryIdCachingProxyHandler;
import com.lyft.data.gateway.router.GatewayBackendManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GatewayBackendManagerImpl implements GatewayBackendManager {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String PRESTO_CLUSTER_STATE_FILE = "presto_cluster_state.json";

  private final ConcurrentMap<String, ProxyBackendConfiguration> backendMap;
  private final String cacheDir;

  public GatewayBackendManagerImpl(List<ProxyBackendConfiguration> backends, String cacheDir) {
    this.cacheDir = cacheDir;
    this.backendMap = new ConcurrentHashMap<>(); // immutable / un-modifiable
    OBJECT_MAPPER.writerWithDefaultPrettyPrinter();

    backends.forEach(
        backend -> {
          backendMap.put(backend.getName(), backend);
        });
    reloadClusterStateAtStartUp();
  }

  public List<ProxyBackendConfiguration> getAllBackends() {
    return ImmutableList.copyOf(backendMap.values());
  }

  public List<ProxyBackendConfiguration> getActiveAdhocBackends() {
    return getActiveBackends(QueryIdCachingProxyHandler.ADHOC_ROUTING_GROUP);
  }

  public List<ProxyBackendConfiguration> getAllActiveBackends() {
    return backendMap
        .values()
        .stream()
        .filter(backend -> backend.isActive())
        .collect(Collectors.toList());
  }

  public List<ProxyBackendConfiguration> getActiveBackends(String routingGroup) {
    return backendMap
        .values()
        .stream()
        .filter(backend -> backend.isActive())
        .filter(backend -> backend.getRoutingGroup().equalsIgnoreCase(routingGroup))
        .collect(Collectors.toList());
  }

  public void deactivateBackend(String backendName) {
    if (!backendMap.containsKey(backendName)) {
      throw new IllegalArgumentException("Backend name [" + backendName + "] not found");
    }
    ProxyBackendConfiguration backendToRemove = backendMap.get(backendName);

    // if adhoc cluster then check availability of at least one cluster before disabling current
    // adhoc cluster
    if (QueryIdCachingProxyHandler.ADHOC_ROUTING_GROUP.equalsIgnoreCase(
        backendToRemove.getRoutingGroup())) {
      List<ProxyBackendConfiguration> activeBackends = getActiveAdhocBackends();
      if (activeBackends.size() <= 1) {
        throw new IllegalArgumentException(
            "Active adhoc backend size is 1, can't deactivate the backend");
      }
    }
    backendToRemove.setActive(false);
    persistClusterState();
    log.info("De-activating backend cluster [{}]", backendName);
  }

  public void activateBackend(String backendName) {
    if (!backendMap.containsKey(backendName)) {
      throw new IllegalArgumentException("Backend name [" + backendName + "] not found");
    }
    ProxyBackendConfiguration toActivate = backendMap.get(backendName);
    toActivate.setActive(true);
    persistClusterState();
  }

  private synchronized void persistClusterState() {
    try (FileWriter fileWriter = new FileWriter(cacheDir + "/" + PRESTO_CLUSTER_STATE_FILE)) {
      String prestoClusterStateJson = OBJECT_MAPPER.writeValueAsString(backendMap);
      fileWriter.write(prestoClusterStateJson);
    } catch (Exception e) {
      log.error("Error saving the cluster state", e);
    }
  }

  private void reloadClusterStateAtStartUp() {
    try (FileReader fileReader = new FileReader(cacheDir + "/" + PRESTO_CLUSTER_STATE_FILE)) {
      StringBuffer sb = new StringBuffer();
      BufferedReader bufferedReader = new BufferedReader(fileReader);
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        sb.append(line).append("\n");
      }
      String prestoClusterStateJson = sb.toString();

      Map<String, ProxyBackendConfiguration> previousClusterStateMap =
          OBJECT_MAPPER.readValue(
              prestoClusterStateJson,
              new TypeReference<Map<String, ProxyBackendConfiguration>>() {});
      previousClusterStateMap.forEach(
          (k, v) -> {
            if (backendMap.containsKey(k)) {
              log.info(
                  "Restoring from previous cluster state : cluster[{}] state [{}]",
                  k,
                  v.isActive());
              backendMap.get(k).setActive(v.isActive());
            }
          });
    } catch (Exception e) {
      log.warn("No previous backend cluster state found - " + e.getMessage());
    }
  }
}
