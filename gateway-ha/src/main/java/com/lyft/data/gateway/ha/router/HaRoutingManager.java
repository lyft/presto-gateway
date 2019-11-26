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
}
