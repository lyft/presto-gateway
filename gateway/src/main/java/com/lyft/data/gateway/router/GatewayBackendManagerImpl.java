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
  private final Set<ProxyBackendConfiguration> activeBackends;

  public GatewayBackendManagerImpl(List<ProxyBackendConfiguration> backends) {
    this.backendNameMap = new HashMap<>();
    this.allBackends = new HashSet<>();
    this.activeBackends = new HashSet<>();
    backends.forEach(
        backend -> {
          backendNameMap.put(backend.getName(), backend);
          allBackends.add(backend);
          // As application starts, it will start with all backends.
          // We can make it agnostic of restart by persisting a copy of active backends to a
          // persistent cache.
          if (backend.isActive()) {
            activeBackends.add(backend);
          }
        });
  }

  public List<ProxyBackendConfiguration> getAllBackends() {
    return ImmutableList.copyOf(allBackends);
  }

  public List<ProxyBackendConfiguration> getActiveBackends() {
    return ImmutableList.copyOf(activeBackends);
  }

  public void deactivateBackend(String backendName) {
    if (!backendNameMap.containsKey(backendName)) {
      throw new IllegalArgumentException("Backend name [" + backendName + "] not found");
    }
    ProxyBackendConfiguration toDeactivate = null;
    for (ProxyBackendConfiguration backend : activeBackends) {
      if (backend.getName().equals(backendName)) {
        toDeactivate = backend;
        break;
      }
    }
    if (toDeactivate != null) {
      if (activeBackends.size() == 1) {
        throw new IllegalArgumentException(
            "Active backend size is 1, can't deactivate the backend");
      }
      activeBackends.remove(toDeactivate);
      log.info("De-activating backend cluster [{}]", backendName);
    }
  }

  public void activateBackend(String backendName) {
    if (!backendNameMap.containsKey(backendName)) {
      throw new IllegalArgumentException("Backend name [" + backendName + "] not found");
    }
    ProxyBackendConfiguration toActivate = backendNameMap.get(backendName);
    if (!activeBackends.contains(toActivate)) {
      activeBackends.add(toActivate);
      log.info("Re-activating backend cluster [{}]", backendName);
    }
  }
}
