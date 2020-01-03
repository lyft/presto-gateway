package com.lyft.data.gateway.ha.router;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HaRoutingManager extends RoutingManager {
  QueryHistoryManager queryHistoryManager;

  public HaRoutingManager(
      GatewayBackendManager gatewayBackendManager, QueryHistoryManager queryHistoryManager) {
    super(gatewayBackendManager);
    this.queryHistoryManager = queryHistoryManager;
  }

  @Override
  protected String findBackendForUnknownQueryId(String queryId) {
    log.debug("Querying history manager for [{}]", queryId);
    String backend;
    backend = queryHistoryManager.getBackendForQueryId(queryId);
    if (Strings.isNullOrEmpty(backend)) {
      backend = super.findBackendForUnknownQueryId(queryId);
    }
    return backend;
  }

}
