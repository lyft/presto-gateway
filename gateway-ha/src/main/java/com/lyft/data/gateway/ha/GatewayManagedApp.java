package com.lyft.data.gateway.ha;

import com.google.inject.Inject;
import com.lyft.data.proxyserver.ProxyServer;
import io.dropwizard.lifecycle.Managed;

public class GatewayManagedApp implements Managed {
  @Inject private ProxyServer gateway;

  @Override
  public void start() {
    if (gateway != null) {
      gateway.start();
    }
  }

  @Override
  public void stop() {
    if (gateway != null) {
      gateway.close();
    }
  }
}
