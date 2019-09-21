package com.lyft.data.gateway.ha.router;

import com.lyft.data.gateway.ha.config.ProxyBackendConfiguration;

import java.util.List;

public interface GatewayBackendManager {
  List<ProxyBackendConfiguration> getAllBackends();

  List<ProxyBackendConfiguration> getAllActiveBackends();

  List<ProxyBackendConfiguration> getActiveAdhocBackends();

  List<ProxyBackendConfiguration> getActiveBackends(String routingGroup);

  ProxyBackendConfiguration addBackend(ProxyBackendConfiguration backend);

  ProxyBackendConfiguration updateBackend(ProxyBackendConfiguration backend);

  void deactivateBackend(String backendName);

  void activateBackend(String backendName);
}
