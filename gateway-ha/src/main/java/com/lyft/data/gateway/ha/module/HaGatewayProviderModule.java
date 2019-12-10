package com.lyft.data.gateway.ha.module;

import com.codahale.metrics.Meter;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.lyft.data.baseapp.AppModule;
import com.lyft.data.gateway.ha.clustermonitor.HealthChecker;
import com.lyft.data.gateway.ha.clustermonitor.PrestoClusterStatsObserver;
import com.lyft.data.gateway.ha.clustermonitor.PrestoQueueLengthChecker;
import com.lyft.data.gateway.ha.config.HaGatewayConfiguration;
import com.lyft.data.gateway.ha.config.RequestRouterConfiguration;
import com.lyft.data.gateway.ha.handler.QueryIdCachingProxyHandler;
import com.lyft.data.gateway.ha.notifier.EmailNotifier;
import com.lyft.data.gateway.ha.persistence.JdbcConnectionManager;
import com.lyft.data.gateway.ha.router.GatewayBackendManager;
import com.lyft.data.gateway.ha.router.HaGatewayManager;
import com.lyft.data.gateway.ha.router.HaQueryHistoryManager;
import com.lyft.data.gateway.ha.router.PrestoQueueLengthRoutingTable;
import com.lyft.data.gateway.ha.router.QueryHistoryManager;
import com.lyft.data.gateway.ha.router.RoutingManager;
import com.lyft.data.proxyserver.ProxyHandler;
import com.lyft.data.proxyserver.ProxyServer;
import com.lyft.data.proxyserver.ProxyServerConfiguration;
import io.dropwizard.setup.Environment;
import java.util.ArrayList;
import java.util.List;

public class HaGatewayProviderModule extends AppModule<HaGatewayConfiguration, Environment> {

  private final JdbcConnectionManager connectionManager;

  public HaGatewayProviderModule(HaGatewayConfiguration configuration, Environment environment) {
    super(configuration, environment);
    connectionManager = new JdbcConnectionManager(configuration.getDataStore());
  }

  protected ProxyHandler getProxyHandler() {
    Meter requestMeter =
        getEnvironment()
            .metrics()
            .meter(getConfiguration().getRequestRouter().getName() + ".requests");
    return new QueryIdCachingProxyHandler(
        getQueryHistoryManager(), getRoutingManager(), getApplicationPort(), requestMeter);
  }

  /**
   * Provides a HA Gateway.
   *
   * @return
   */
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
    return new HaGatewayManager(getConnectionManager());
  }

  @Provides
  @Singleton
  public QueryHistoryManager getQueryHistoryManager() {
    return new HaQueryHistoryManager(getConnectionManager());
  }

  @Provides
  @Singleton
  public JdbcConnectionManager getConnectionManager() {
    return this.connectionManager;
  }

  @Provides
  @Singleton
  public RoutingManager getRoutingManager() {
    return new PrestoQueueLengthRoutingTable(getGatewayBackendManager(), getQueryHistoryManager());
  }

  /**
   * Injects observers of the
   * {@link com.lyft.data.gateway.ha.clustermonitor.ActiveClusterMonitor} that can perform a custom
   * action in reponse to the status updates.
   *
   * @return
   */
  @Provides
  @Singleton
  public List<PrestoClusterStatsObserver> getClusterStatsObservers() {
    List<PrestoClusterStatsObserver> observers = new ArrayList<>();
    observers.add(new HealthChecker(new EmailNotifier(getConfiguration().getNotifier())));
    observers.add(new
        PrestoQueueLengthChecker((PrestoQueueLengthRoutingTable) getRoutingManager()));
    return observers;
  }


}
