package com.lyft.data.gateway.module;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.lyft.data.baseapp.AppModule;
import com.lyft.data.gateway.config.GatewayConfiguration;
import com.lyft.data.proxyserver.ProxyHandler;
import com.lyft.data.proxyserver.ProxyServer;
import com.lyft.data.proxyserver.ProxyServerConfiguration;
import io.dropwizard.setup.Environment;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxyBackendProviderModule extends AppModule<GatewayConfiguration, Environment> {
  public ProxyBackendProviderModule(GatewayConfiguration configuration, Environment environment) {
    super(configuration, environment);
  }

  protected ProxyHandler getProxyHandler(ProxyServerConfiguration proxyServerConfiguration) {
    return new ProxyHandler();
  }

  @Provides
  @Singleton
  public List<ProxyServer> getProxyServers() {
    List<ProxyServer> proxyServers = new ArrayList<>();
    // Setting up presto backends
    for (ProxyServerConfiguration proxyServerConfiguration : getConfiguration().getBackends()) {
      log.debug("Starting proxy backend [{}]", proxyServerConfiguration.getName());
      ProxyServer proxyServer =
          new ProxyServer(proxyServerConfiguration, getProxyHandler(proxyServerConfiguration));
      proxyServers.add(proxyServer);
    }
    return proxyServers;
  }
}
