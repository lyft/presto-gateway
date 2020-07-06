package com.lyft.data.gateway.ha.config;

import com.lyft.data.proxyserver.ProxyServerConfiguration;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProxyBackendConfiguration extends ProxyServerConfiguration {
  private boolean active = true;
  private String routingGroup = "adhoc";
  private String externalUrl;

  public String getExternalUrl() {
    if (externalUrl == null) {
      return getProxyTo();
    }
    return externalUrl;
  }

}
