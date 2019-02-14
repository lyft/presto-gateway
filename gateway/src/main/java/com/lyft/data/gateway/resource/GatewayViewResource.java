package com.lyft.data.gateway.resource;

import com.google.inject.Inject;
import com.lyft.data.gateway.config.ProxyBackendConfiguration;
import com.lyft.data.gateway.router.GatewayBackendManager;
import com.lyft.data.gateway.router.QueryHistoryManager;
import io.dropwizard.views.View;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    gatewayView.setQueryDistribution(getQueryHistoryDistribution());
    return gatewayView;
  }

  @GET
  @Path("api/queryHistory")
  @Produces(MediaType.APPLICATION_JSON)
  public List<QueryHistoryManager.QueryDetail> getQueryHistory() {
    return queryHistoryManager.fetchQueryHistory();
  }

  @GET
  @Path("api/activeBackends")
  @Produces(MediaType.APPLICATION_JSON)
  public List<ProxyBackendConfiguration> getActiveBackends() {
    return gatewayBackendManager
        .getAllBackends()
        .stream()
        .filter(b -> b.isActive())
        .collect(Collectors.toList());
  }

  @GET
  @Path("api/queryHistoryDistribution")
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, Integer> getQueryHistoryDistribution() {
    Map<String, String> urlToNameMap = new HashMap<>();
    gatewayBackendManager
        .getAllBackends()
        .stream()
        .forEach(
            backend -> {
              urlToNameMap.put(backend.getProxyTo(), backend.getName());
            });

    Map<String, Integer> clusterToQueryCount = new HashMap<>();
    queryHistoryManager
        .fetchQueryHistory()
        .forEach(
            q -> {
              String cluster = urlToNameMap.get(q.getBackendUrl());
              if (cluster == null) {
                cluster = q.getBackendUrl();
              }
              if (!clusterToQueryCount.containsKey(cluster)) {
                clusterToQueryCount.put(cluster, 0);
              }
              clusterToQueryCount.put(cluster, clusterToQueryCount.get(cluster) + 1);
            });
    return clusterToQueryCount;
  }

  @Data
  public static class GatewayView extends View {
    private final long gatewayStartTime = START_TIME;
    private List<ProxyBackendConfiguration> backendConfigurations;
    private List<QueryHistoryManager.QueryDetail> queryHistory;
    private Map<String, Integer> queryDistribution;

    protected GatewayView(String templateName) {
      super(templateName, Charset.defaultCharset());
    }
  }
}
