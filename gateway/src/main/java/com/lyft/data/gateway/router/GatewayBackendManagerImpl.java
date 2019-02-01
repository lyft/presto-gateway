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
  private final ConcurrentMap<String, ProxyBackendConfiguration> backendNameMap;

  public GatewayBackendManagerImpl(List<ProxyBackendConfiguration> backends) {
    this.backendNameMap = new ConcurrentHashMap<>(); // immutable / un-modifiable

    backends.forEach(
        backend -> {
          backendNameMap.put(backend.getName(), backend);
        });
  }

  public List<ProxyBackendConfiguration> getAllBackends() {
    return ImmutableList.copyOf(backendNameMap.values());
  }

  public List<ProxyBackendConfiguration> getActiveAdhocBackends() {
    return backendNameMap
        .values()
        .stream()
        .filter(backend -> backend.isActive())
        .filter(backend -> !backend.isScheduledCluster())
        .collect(Collectors.toList());
  }

  public List<ProxyBackendConfiguration> getActiveScheduledBackends() {
    return backendNameMap
        .values()
        .stream()
        .filter(backend -> backend.isActive())
        .filter(backend -> backend.isScheduledCluster())
        .collect(Collectors.toList());
  }

  public void deactivateBackend(String backendName) {
    if (!backendNameMap.containsKey(backendName)) {
      throw new IllegalArgumentException("Backend name [" + backendName + "] not found");
    }
    ProxyBackendConfiguration backendToRemove = backendNameMap.get(backendName);
    backendToRemove.setActive(false);
    log.info("De-activating backend cluster [{}]", backendName);
  }

  public void activateBackend(String backendName) {
    if (!backendNameMap.containsKey(backendName)) {
      throw new IllegalArgumentException("Backend name [" + backendName + "] not found");
    }
    ProxyBackendConfiguration toActivate = backendNameMap.get(backendName);
    toActivate.setActive(true);
  }
}
