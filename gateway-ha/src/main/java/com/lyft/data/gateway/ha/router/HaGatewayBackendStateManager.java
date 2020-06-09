package com.lyft.data.gateway.ha.router;

import com.lyft.data.gateway.ha.persistence.JdbcConnectionManager;
import com.lyft.data.gateway.ha.persistence.dao.GatewayBackendStateHistory;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HaGatewayBackendStateManager implements GatewayBackendStateManager {

  private JdbcConnectionManager connectionManager;

  public HaGatewayBackendStateManager(JdbcConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
  }

  @Override
  public List<GatewayBackendState> getAllBackendStates() {
    //TODO implement method to obtain all cluster states
    try {
      connectionManager.open();
      List<GatewayBackendStateHistory> proxyBackendList = GatewayBackendStateHistory.findAll();
      return GatewayBackendStateHistory.upcast(proxyBackendList);
    } finally {
      connectionManager.close();
    }
  }

  @Override
  public GatewayBackendState addBackend(GatewayBackendState backend) {
    try {
      connectionManager.open();
      GatewayBackendStateHistory.create(new GatewayBackendStateHistory(), backend);
    } finally {
      connectionManager.close();
    }

    return backend;
  }
}
