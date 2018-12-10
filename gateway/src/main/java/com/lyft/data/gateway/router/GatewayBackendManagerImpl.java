package com.lyft.data.gateway.router;

import com.google.common.collect.ImmutableList;
import com.lyft.data.gateway.config.ProxyBackendConfiguration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GatewayBackendManagerImpl implements GatewayBackendManager {
  private final Map<String, ProxyBackendConfiguration> backendNameMap;
  private final Set<ProxyBackendConfiguration> allBackends;
  private final Set<ProxyBackendConfiguration> activeAdhocBackends;
  private final Set<ProxyBackendConfiguration> activeScheduledBackends;

  public GatewayBackendManagerImpl(List<ProxyBackendConfiguration> backends) {
    this.backendNameMap = new HashMap<>();
    this.allBackends = new HashSet<>();
    this.activeAdhocBackends = new HashSet<>();
    this.activeScheduledBackends = new HashSet<>();
    backends.forEach(
        backend -> {
          backendNameMap.put(backend.getName(), backend);
          allBackends.add(backend);
          // As application starts, it will start with all backends.
          // We can make it agnostic of restart by persisting a copy of active backends to a
          // persistent cache.
          if (backend.isActive()) {
            if (backend.isScheduledCluster()) {
              activeScheduledBackends.add(backend);
            } else {
              activeAdhocBackends.add(backend);
            }
          }
        });
  }

  public List<ProxyBackendConfiguration> getAllBackends() {
    return ImmutableList.copyOf(allBackends);
  }

  public List<ProxyBackendConfiguration> getActiveAdhocBackends() {
    return ImmutableList.copyOf(activeAdhocBackends);
  }

  public List<ProxyBackendConfiguration> getActiveScheduledBackends() {
    return ImmutableList.copyOf(activeScheduledBackends);
  }

  public void deactivateBackend(String backendName) {
    if (!backendNameMap.containsKey(backendName)) {
      throw new IllegalArgumentException("Backend name [" + backendName + "] not found");
    }
    ProxyBackendConfiguration toDeactivate = null;
    for (ProxyBackendConfiguration backend : activeAdhocBackends) {
      if (backend.getName().equals(backendName)) {
        toDeactivate = backend;
        break;
      }
    }
    if (toDeactivate == null) {
      for (ProxyBackendConfiguration backend : activeScheduledBackends) {
        if (backend.getName().equals(backendName)) {
          toDeactivate = backend;
          break;
        }
      }
    }
    if (toDeactivate != null) {
      if (toDeactivate.isScheduledCluster() == false) {
        if (activeAdhocBackends.size() == 1) {
          throw new IllegalArgumentException(
              "Active backend size is 1, can't deactivate the backend");
        }
        activeAdhocBackends.remove(toDeactivate);
        log.info("De-activating backend cluster [{}]", backendName);
      } else {
        activeScheduledBackends.remove(toDeactivate);
      }
    }
  }

  public void activateBackend(String backendName) {
    if (!backendNameMap.containsKey(backendName)) {
      throw new IllegalArgumentException("Backend name [" + backendName + "] not found");
    }
    ProxyBackendConfiguration toActivate = backendNameMap.get(backendName);
    if (toActivate.isScheduledCluster()) {
      if (!activeScheduledBackends.contains(toActivate)) {
        activeScheduledBackends.add(toActivate);
        log.info("Re-activating Scheduled backend cluster [{}]", backendName);
      }
    } else {
      if (!activeAdhocBackends.contains(toActivate)) {
        activeAdhocBackends.add(toActivate);
        log.info("Re-activating Adhoc backend cluster [{}]", backendName);
      }
    }
  }
}
