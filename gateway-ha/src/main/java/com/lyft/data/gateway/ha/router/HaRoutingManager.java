package com.lyft.data.gateway.ha.router;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HaRoutingManager extends RoutingManager {
  HaQueryHistoryManager queryHistoryManager;

  public HaRoutingManager(
      GatewayBackendManager gatewayBackendManager, HaQueryHistoryManager queryHistoryManager) {
    super(gatewayBackendManager);
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
