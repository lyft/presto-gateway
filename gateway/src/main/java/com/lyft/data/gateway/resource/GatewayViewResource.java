package com.lyft.data.gateway.resource;

import com.google.inject.Inject;
import com.lyft.data.gateway.config.ProxyBackendConfiguration;
import com.lyft.data.gateway.router.GatewayBackendManager;
import com.lyft.data.gateway.router.QueryHistoryManager;
import io.dropwizard.views.View;

import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import lombok.Data;

@Path("/")
@Produces(MediaType.TEXT_HTML)
public class GatewayViewResource {
  private static final long START_TIME = System.currentTimeMillis();
  @Inject private GatewayBackendManager gatewayBackendManager;
  @Inject private QueryHistoryManager queryHistoryManager;

  @GET
  public GatewayView getAdminUi() {
    GatewayView gatewayView = new GatewayView("/template/gateway-view.ftl");
    // Get All active backends
    gatewayView.setBackendConfigurations(
        gatewayBackendManager
            .getAllBackends()
            .stream()
            .filter(b -> b.isActive())
            .collect(Collectors.toList()));

    gatewayView.setQueryHistory(queryHistoryManager.fetchQueryHistory());

    return gatewayView;
  }

  @Data
  public static class GatewayView extends View {
    private final long gatewayStartTime = START_TIME;
    private List<ProxyBackendConfiguration> backendConfigurations;
    private List<QueryHistoryManager.QueryDetail> queryHistory;

    protected GatewayView(String templateName) {
      super(templateName, Charset.defaultCharset());
    }
  }
}
