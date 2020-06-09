package com.lyft.data.gateway.ha.persistence.dao;

import com.lyft.data.gateway.ha.router.GatewayBackendStateManager.GatewayBackendState;
import java.util.ArrayList;
import java.util.List;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Cached;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@Table("gateway_backend_state")
@IdName("name")
@Cached
public class GatewayBackendStateHistory extends Model {
  //TODO: Create an active jdbc model along with a function to create and update entries
  private static final String name = "name";
  private static final String health = "health";
  private static final String workerCount = "worker_count";

  public static List<GatewayBackendState> upcast(List<GatewayBackendStateHistory>
      gatewayBackendStateHistoryList) {
    List<GatewayBackendState> gatewayBackendStates = new ArrayList<>();
    for (GatewayBackendStateHistory dao : gatewayBackendStateHistoryList) {
      GatewayBackendState gatewayBackendState = new GatewayBackendState();
      gatewayBackendState.setName(dao.getString(name));
      gatewayBackendState.setHealth(dao.getBoolean(health));
      gatewayBackendState.setWorkerCount(dao.getInteger(workerCount));
    }
    return gatewayBackendStates;
  }

  public static void create(GatewayBackendStateHistory model,
      GatewayBackendState gatewayBackendState) {
    model.set(name, gatewayBackendState.getName());
    model.set(health, gatewayBackendState.getHealth());
    model.set(workerCount, gatewayBackendState.getWorkerCount());
    model.insert();
  }
}
