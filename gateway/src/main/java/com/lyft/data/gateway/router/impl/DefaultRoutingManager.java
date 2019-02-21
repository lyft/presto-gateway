package com.lyft.data.gateway.router.impl;

import com.lyft.data.gateway.router.GatewayBackendManager;
import com.lyft.data.gateway.router.RoutingManager;

public class DefaultRoutingManager extends RoutingManager {
  public DefaultRoutingManager(GatewayBackendManager gatewayBackendManager, String cacheDataDir) {
    super(gatewayBackendManager, cacheDataDir);
  }
}
