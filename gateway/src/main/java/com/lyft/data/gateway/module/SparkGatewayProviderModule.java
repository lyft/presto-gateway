package com.lyft.data.gateway.module;

import com.lyft.data.gateway.config.GatewayConfiguration;
import com.lyft.data.gateway.config.RequestRouterConfiguration;
import com.lyft.data.gateway.handler.SparkGatewayRoutingHandler;
import com.lyft.data.gateway.router.GatewayBackendManager;
import com.lyft.data.proxyserver.ProxyHandler;
import com.lyft.data.proxyserver.ProxyServerConfiguration;
import io.dropwizard.setup.Environment;

public class SparkGatewayProviderModule extends GatewayProviderModule {
  public SparkGatewayProviderModule(GatewayConfiguration configuration, Environment environment) {
    super(configuration, environment);
  }

  @Override
  protected ProxyServerConfiguration getGatewayProxyConfig() {
    RequestRouterConfiguration routerConfiguration = getConfiguration().getRequestRouter();

    ProxyServerConfiguration routerProxyConfig = new ProxyServerConfiguration();
    routerProxyConfig.setLocalPort(routerConfiguration.getPort());
    routerProxyConfig.setName(routerConfiguration.getName());
    routerProxyConfig.setProxyTo("");
    routerProxyConfig.setPreserveHost("false");
    return routerProxyConfig;
  }

  protected ProxyHandler getProxyHandler() {
    GatewayBackendManager gatewayBackendManager = getGatewayBackendManager();
    return new SparkGatewayRoutingHandler(gatewayBackendManager);
  }
}
