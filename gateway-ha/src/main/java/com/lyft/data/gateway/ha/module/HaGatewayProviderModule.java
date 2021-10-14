package com.lyft.data.gateway.ha.module;

import com.codahale.metrics.Meter;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.lyft.data.baseapp.AppModule;
import com.lyft.data.gateway.ha.config.HaGatewayConfiguration;
import com.lyft.data.gateway.ha.config.RequestRouterConfiguration;
import com.lyft.data.gateway.ha.config.RoutingRulesConfiguration;
import com.lyft.data.gateway.ha.handler.QueryIdCachingProxyHandler;
import com.lyft.data.gateway.ha.persistence.JdbcConnectionManager;
import com.lyft.data.gateway.ha.router.GatewayBackendManager;
import com.lyft.data.gateway.ha.router.HaGatewayManager;
import com.lyft.data.gateway.ha.router.HaQueryHistoryManager;
import com.lyft.data.gateway.ha.router.HaResourceGroupsManager;
import com.lyft.data.gateway.ha.router.HaRoutingManager;
import com.lyft.data.gateway.ha.router.QueryHistoryManager;
import com.lyft.data.gateway.ha.router.ResourceGroupsManager;
import com.lyft.data.gateway.ha.router.RoutingGroupSelector;
import com.lyft.data.gateway.ha.router.RoutingManager;
import com.lyft.data.proxyserver.ProxyHandler;
import com.lyft.data.proxyserver.ProxyServer;
import com.lyft.data.proxyserver.ProxyServerConfiguration;
import io.dropwizard.setup.Environment;
import java.io.FileReader;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.mvel.MVELRuleFactory;
import org.jeasy.rules.support.reader.YamlRuleDefinitionReader;

public class HaGatewayProviderModule extends AppModule<HaGatewayConfiguration, Environment> {

  private final ResourceGroupsManager resourceGroupsManager;
  private final GatewayBackendManager gatewayBackendManager;
  private final QueryHistoryManager queryHistoryManager;
  private final RoutingManager routingManager;
  private final JdbcConnectionManager connectionManager;

  public HaGatewayProviderModule(HaGatewayConfiguration configuration, Environment environment) {
    super(configuration, environment);
    connectionManager = new JdbcConnectionManager(configuration.getDataStore());
    resourceGroupsManager = new HaResourceGroupsManager(connectionManager);
    gatewayBackendManager = new HaGatewayManager(connectionManager);
    queryHistoryManager = new HaQueryHistoryManager(connectionManager);
    routingManager =
        new HaRoutingManager(gatewayBackendManager, (HaQueryHistoryManager) queryHistoryManager);
  }

  protected ProxyHandler getProxyHandler() {
    Meter requestMeter =
        getEnvironment()
            .metrics()
            .meter(getConfiguration().getRequestRouter().getName() + ".requests");

    RoutingRulesConfiguration routingRulesConfig = getConfiguration().getRoutingRules();
    RoutingGroupSelector routingGroupSelector = RoutingGroupSelector.byRoutingGroupHeader();

    if (routingRulesConfig.isRulesEngineEnabled()) {
      String rulesConfigPath = routingRulesConfig.getRulesConfigPath();

      try {
        MVELRuleFactory ruleFactory = new MVELRuleFactory(new YamlRuleDefinitionReader());
        Rules rules = ruleFactory.createRules(
            new FileReader(rulesConfigPath));
        routingGroupSelector = RoutingGroupSelector.byRoutingRulesEngine(rules);
      } catch (Exception e) {
        System.out.println(
            "Error opening rules configuration file %s."
            + "Using routing group header as default..".format(rulesConfigPath));
      }
    }

    return new QueryIdCachingProxyHandler(
        getQueryHistoryManager(),
        getRoutingManager(),
        routingGroupSelector,
        getApplicationPort(),
        requestMeter);
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
      routerProxyConfig.setPreserveHost("false");
      ProxyHandler proxyHandler = getProxyHandler();
      gateway = new ProxyServer(routerProxyConfig, proxyHandler);
    }
    return gateway;
  }

  @Provides
  @Singleton
  public ResourceGroupsManager getResourceGroupsManager() {
    return this.resourceGroupsManager;
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

  @Provides
  @Singleton
  public RoutingManager getRoutingManager() {
    return this.routingManager;
  }

  @Provides
  @Singleton
  public JdbcConnectionManager getConnectionManager() {
    return this.connectionManager;
  }
}
