package com.lyft.data.gateway.ha.router;

import com.google.common.base.Strings;
import com.lyft.data.gateway.router.GatewayBackendManager;
import com.lyft.data.gateway.router.RoutingManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HaRoutingManager extends RoutingManager {
  HaQueryHistoryManager queryHistoryManager;

  public HaRoutingManager(
      GatewayBackendManager gatewayBackendManager,
      HaQueryHistoryManager queryHistoryManager,
      String cacheDataDir) {
    super(gatewayBackendManager, cacheDataDir);
    this.queryHistoryManager = queryHistoryManager;
  }

  protected String findBackendForUnknownQueryId(String queryId) {
    String backend;
    backend = queryHistoryManager.getBackendForQueryId(queryId);
    if (Strings.isNullOrEmpty(backend)) {
      backend = super.findBackendForUnknownQueryId(queryId);
    }
    return backend;
  }
}
