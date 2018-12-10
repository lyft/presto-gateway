package com.lyft.data.gateway.router;

import com.lyft.data.gateway.config.ProxyBackendConfiguration;

import java.util.List;

public interface GatewayBackendManager {
  List<ProxyBackendConfiguration> getAllBackends();

  List<ProxyBackendConfiguration> getActiveAdhocBackends();

  List<ProxyBackendConfiguration> getActiveScheduledBackends();

  void deactivateBackend(String backendName);

  void activateBackend(String backendName);
}
