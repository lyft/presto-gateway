package com.lyft.data.gateway.handler;

import com.lyft.data.gateway.config.ProxyBackendConfiguration;
import com.lyft.data.gateway.router.GatewayBackendManager;
import com.lyft.data.proxyserver.ProxyHandler;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpHeaders;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;



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

  @Override
  public void preConnectionHook(HttpServletRequest request, Request proxyRequest) {
    proxyRequest.header(
        HttpHeader.PROXY_AUTHORIZATION, request.getHeader(HttpHeaders.AUTHORIZATION));
  }

  @Override
  protected String rewriteTarget(HttpServletRequest request) {
    // Dont override this unless absolutely needed.
    String targetLabel = request.getHeader(TARGET_HEADER);
    String targetUri = "";
    if (targetLabel == null) {
      targetUri = gatewayBackendManager.getActiveBackends().get(0).getProxyTo();
    } else {
      ProxyBackendConfiguration targetDestConfig = backendNameMap.get(targetLabel);
      targetUri = targetDestConfig.getProxyTo();
    }
    return targetUri
        + request.getRequestURI()
        + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
  }
}
