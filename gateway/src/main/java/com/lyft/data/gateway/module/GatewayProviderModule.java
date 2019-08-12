package com.lyft.data.gateway.module;

import com.codahale.metrics.Meter;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.lyft.data.baseapp.AppModule;
import com.lyft.data.gateway.config.GatewayConfiguration;
import com.lyft.data.gateway.config.ProxyBackendConfiguration;
import com.lyft.data.gateway.config.RequestRouterConfiguration;
import com.lyft.data.gateway.handler.QueryIdCachingProxyHandler;
import com.lyft.data.gateway.router.GatewayBackendManager;
import com.lyft.data.gateway.router.QueryHistoryManager;
import com.lyft.data.gateway.router.impl.GatewayBackendManagerImpl;
import com.lyft.data.gateway.router.impl.QueryHistoryManagerImpl;
import com.lyft.data.proxyserver.ProxyHandler;
import com.lyft.data.proxyserver.ProxyServer;
import com.lyft.data.proxyserver.ProxyServerConfiguration;
import io.dropwizard.setup.Environment;

import java.util.ArrayList;
import java.util.List;

public class GatewayProviderModule extends AppModule<GatewayConfiguration, Environment> {

  private final List<ProxyBackendConfiguration> gatewayBackends;

  private final GatewayBackendManager gatewayBackendManager;
  private final QueryHistoryManager queryHistoryManager;

  public GatewayProviderModule(GatewayConfiguration configuration, Environment environment) {
    super(configuration, environment);
    this.gatewayBackends = new ArrayList<>();
    for (ProxyBackendConfiguration backend : getConfiguration().getBackends()) {
      if (backend.isIncludeInRouter()) {
        gatewayBackends.add(backend);
      }
    }
    this.gatewayBackendManager =
        new GatewayBackendManagerImpl(
            this.gatewayBackends, configuration.getRequestRouter().getCacheDir());
    this.queryHistoryManager =
        new QueryHistoryManagerImpl(configuration.getRequestRouter().getHistorySize());

  }

  /* @return Provides instance of RoutingProxyHandler. */

  protected ProxyHandler getProxyHandler() {
    Meter requestMeter =
        getEnvironment()
            .metrics()
            .meter(getConfiguration().getRequestRouter().getName() + ".requests");
    // Return the Proxy Handler for RequestRouter.
    return new QueryIdCachingProxyHandler(
        gatewayBackendManager, queryHistoryManager, getConfiguration(), requestMeter);
  }

  protected ProxyServerConfiguration getGatewayProxyConfig() {
    RequestRouterConfiguration routerConfiguration = getConfiguration().getRequestRouter();

    ProxyServerConfiguration routerProxyConfig = new ProxyServerConfiguration();
    routerProxyConfig.setLocalPort(routerConfiguration.getPort());
    routerProxyConfig.setName(routerConfiguration.getName());
    routerProxyConfig.setProxyTo("");
    return routerProxyConfig;
  }

  @Provides
  @Singleton
  public ProxyServer provideGateway() {
    ProxyServer gateway = null;
    if (getConfiguration().getRequestRouter() != null) {
      // Setting up request router
      RequestRouterConfiguration routerConfiguration = getConfiguration().getRequestRouter();

      ProxyServerConfiguration routerProxyConfig = new ProxyServerConfiguration();
      routerProxyConfig.setLocalPort(routerConfiguration.getPort());
      routerProxyConfig.setName(routerConfiguration.getName());
      routerProxyConfig.setProxyTo("");
      routerProxyConfig.setSsl(routerConfiguration.isSsl());
      routerProxyConfig.setKeystorePath(routerConfiguration.getKeystorePath());
      routerProxyConfig.setKeystorePass(routerConfiguration.getKeystorePass());

      ProxyHandler proxyHandler = getProxyHandler();
      gateway = new ProxyServer(routerProxyConfig, proxyHandler);
    }
    return gateway;
  }

  @Provides
  @Singleton
  public GatewayBackendManager getGatewayBackendManager() {
    return this.gatewayBackendManager;
  }

  @Provides
  @Singleton
  public QueryHistoryManager getQueryHistoryManager() {
    return this.queryHistoryManager;
  }
}
