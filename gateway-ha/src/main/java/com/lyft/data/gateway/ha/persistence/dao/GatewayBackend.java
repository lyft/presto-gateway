package com.lyft.data.gateway.ha.persistence.dao;

import com.lyft.data.gateway.ha.config.ProxyBackendConfiguration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Cached;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@Slf4j
@Table("gateway_backend")
@IdName("name")
@Cached
public class GatewayBackend extends Model {
  private static final String name = "name";
  private static final String routingGroup = "routing_group";
  private static final String backendUrl = "backend_url";
  private static final String active = "active";

  public static List<ProxyBackendConfiguration> upcast(List<GatewayBackend> gatewayBackendList) {
    List<ProxyBackendConfiguration> proxyBackendConfigurations = new ArrayList<>();
    for (GatewayBackend model : gatewayBackendList) {
      ProxyBackendConfiguration backendConfig = new ProxyBackendConfiguration();
      backendConfig.setActive(model.getBoolean(active));
      backendConfig.setRoutingGroup(model.getString(routingGroup));
      backendConfig.setProxyTo(model.getString(backendUrl));
      backendConfig.setName(model.getString(name));
      proxyBackendConfigurations.add(backendConfig);
    }
    return proxyBackendConfigurations;
  }

  public static void update(GatewayBackend model, ProxyBackendConfiguration backend) {
    model
        .set(name, backend.getName())
        .set(routingGroup, backend.getRoutingGroup())
        .set(backendUrl, backend.getProxyTo())
        .set(active, backend.isActive())
        .saveIt();
  }

  public static void create(GatewayBackend model, ProxyBackendConfiguration backend) {
    model
        .create(
            name,
            backend.getName(),
            routingGroup,
            backend.getRoutingGroup(),
            backendUrl,
            backend.getProxyTo(),
            active,
            backend.isActive())
        .insert();
  }
}
