package com.lyft.data.gateway.router;

import com.google.common.collect.ImmutableList;
import com.lyft.data.gateway.config.ProxyBackendConfiguration;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GatewayBackendManagerImpl implements GatewayBackendManager {
  private final ConcurrentMap<String, ProxyBackendConfiguration> backendMap;

  public GatewayBackendManagerImpl(List<ProxyBackendConfiguration> backends) {
    this.backendMap = new ConcurrentHashMap<>(); // immutable / un-modifiable

    backends.forEach(
        backend -> {
          backendMap.put(backend.getName(), backend);
        });
  }

  public List<ProxyBackendConfiguration> getAllBackends() {
    return ImmutableList.copyOf(backendMap.values());
  }

  public List<ProxyBackendConfiguration> getActiveAdhocBackends() {
    return backendMap
        .values()
        .stream()
        .filter(backend -> backend.isActive())
        .filter(backend -> !backend.isScheduledCluster())
        .collect(Collectors.toList());
  }

  public List<ProxyBackendConfiguration> getActiveScheduledBackends() {
    return backendMap
        .values()
        .stream()
        .filter(backend -> backend.isActive())
        .filter(backend -> backend.isScheduledCluster())
        .collect(Collectors.toList());
  }

  public void deactivateBackend(String backendName) {
    if (!backendMap.containsKey(backendName)) {
      throw new IllegalArgumentException("Backend name [" + backendName + "] not found");
    }
    ProxyBackendConfiguration backendToRemove = backendMap.get(backendName);
    if (!backendToRemove.isScheduledCluster()) {
      List<ProxyBackendConfiguration> activeBackends = getActiveAdhocBackends();
      if (activeBackends.size() <= 1) {
        throw new IllegalArgumentException(
            "Active backend size is 1, can't deactivate the backend");
      }
    }
    backendToRemove.setActive(false);
    log.info("De-activating backend cluster [{}]", backendName);
  }

  public void activateBackend(String backendName) {
    if (!backendMap.containsKey(backendName)) {
      throw new IllegalArgumentException("Backend name [" + backendName + "] not found");
    }
    ProxyBackendConfiguration toActivate = backendMap.get(backendName);
    toActivate.setActive(true);
  }
}
