package com.lyft.data.spark.gateway.handler;

import com.lyft.data.gateway.config.ProxyBackendConfiguration;
import com.lyft.data.gateway.router.GatewayBackendManager;
import com.lyft.data.proxyserver.ProxyHandler;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpServletRequest;

public class SparkGatewayRoutingHandler extends ProxyHandler {
  final GatewayBackendManager gatewayBackendManager;
  final ConcurrentMap<String, ProxyBackendConfiguration> backendNameMap;
  private static final String TARGET_HEADER = "label";

  public SparkGatewayRoutingHandler(GatewayBackendManager gatewayBackendManager) {
    this.gatewayBackendManager = gatewayBackendManager;
    this.backendNameMap = new ConcurrentHashMap<>();
    List<ProxyBackendConfiguration> configurations = gatewayBackendManager.getAllBackends();
    configurations.forEach(c -> backendNameMap.put(c.getName(), c));
  }

  protected String rewriteTarget(HttpServletRequest request) {
    // Dont override this unless absolutely needed.
    String targetLabel = request.getHeader(TARGET_HEADER);
    ProxyBackendConfiguration targetDest = backendNameMap.get(targetLabel);
    if (targetLabel == null || targetDest == null) {
      return gatewayBackendManager.getActiveBackends().get(0).getProxyTo();
    }
    return targetDest.getProxyTo();
  }
}
