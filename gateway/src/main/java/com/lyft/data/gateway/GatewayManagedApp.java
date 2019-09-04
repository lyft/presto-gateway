package com.lyft.data.gateway;

import com.google.inject.Inject;
import com.lyft.data.proxyserver.ProxyServer;
import io.dropwizard.lifecycle.Managed;

import java.util.List;

public class GatewayManagedApp implements Managed {
  @Inject private List<ProxyServer> proxyServers;
  @Inject private ProxyServer gateway;

  @Override
  public void start() {
    if (gateway != null) {
      gateway.start();
    }

    if (proxyServers != null) {
      proxyServers.forEach(ProxyServer::start);
    }
  }

  @Override
  public void stop() {
    if (gateway != null) {
      gateway.close();
    }
    if (proxyServers != null) {
      proxyServers.forEach(ProxyServer::close);
    }
  }
}
