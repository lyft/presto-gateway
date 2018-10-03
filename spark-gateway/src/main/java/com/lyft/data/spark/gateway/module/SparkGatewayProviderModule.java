package com.lyft.data.spark.gateway.module;

import com.lyft.data.gateway.config.GatewayConfiguration;
import com.lyft.data.gateway.module.GatewayProviderModule;
import com.lyft.data.gateway.router.GatewayBackendManager;
import com.lyft.data.proxyserver.ProxyHandler;
import com.lyft.data.spark.gateway.handler.SparkGatewayRoutingHandler;

import io.dropwizard.setup.Environment;

public class SparkGatewayProviderModule extends GatewayProviderModule {
  public SparkGatewayProviderModule(GatewayConfiguration configuration, Environment environment) {
    super(configuration, environment);
  }

  protected ProxyHandler getProxyHandler() {
    GatewayBackendManager gatewayBackendManager = getGatewayBackendManager();
    return new SparkGatewayRoutingHandler(gatewayBackendManager);
  }
}
