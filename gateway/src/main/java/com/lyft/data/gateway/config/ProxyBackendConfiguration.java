package com.lyft.data.gateway.config;

import com.lyft.data.proxyserver.ProxyServerConfiguration;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true, exclude = {"active"})
public class ProxyBackendConfiguration extends ProxyServerConfiguration {
  private boolean includeInRouter = true;
  private boolean active = true;
  private String routingGroup = "adhoc";
}
