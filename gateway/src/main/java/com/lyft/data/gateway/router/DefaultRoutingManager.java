package com.lyft.data.gateway.router;

public class DefaultRoutingManager extends RoutingManager {
  public DefaultRoutingManager(GatewayBackendManager gatewayBackendManager, String cacheDataDir) {
    super(gatewayBackendManager, cacheDataDir);
  }
}
