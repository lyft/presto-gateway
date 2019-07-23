package com.lyft.data.gateway.router;

import com.lyft.data.gateway.config.ProxyBackendConfiguration;

import java.util.List;

public interface GatewayBackendManager {
  List<ProxyBackendConfiguration> getAllBackends();

  List<ProxyBackendConfiguration> getAllActiveBackends();

  List<ProxyBackendConfiguration> getActiveAdhocBackends();

  List<ProxyBackendConfiguration> getActiveBackends(String routingGroup);

  void deactivateBackend(String backendName);

  void activateBackend(String backendName);
}
