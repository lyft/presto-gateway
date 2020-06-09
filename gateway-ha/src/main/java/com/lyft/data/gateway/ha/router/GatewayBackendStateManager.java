package com.lyft.data.gateway.ha.router;

//import com.lyft.data.gateway.ha.persistence.dao.GatewayBackendStateHistory;
import java.util.List;
import lombok.Data;
import lombok.ToString;

public interface GatewayBackendStateManager {

  List<GatewayBackendState> getAllBackendStates();

  GatewayBackendState addBackend(GatewayBackendState backend);

  @Data
  @ToString
  class GatewayBackendState {

    private String name;
    private Boolean health;
    private int workerCount;

  }

}
