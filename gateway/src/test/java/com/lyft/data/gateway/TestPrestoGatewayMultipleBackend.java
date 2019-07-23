package com.lyft.data.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.lyft.data.gateway.config.ProxyBackendConfiguration;
import java.io.IOException;
import java.util.Arrays;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TestPrestoGatewayMultipleBackend {
  public static final String EXPECTED_RESPONSE1 = "{\"id\":\"testId1\"}";
  public static final String EXPECTED_RESPONSE2 = "{\"id\":\"testId2\"}";

  final int routerPort = 20000 + (int) (Math.random() * 1000);
  final int backend1Port = 21000 + (int) (Math.random() * 1000);
  final int backend2Port = 21000 + (int) (Math.random() * 1000);

  private WireMockServer adhocBackend;
  private WireMockServer scheduledBackend;

  @BeforeClass(alwaysRun = true)
  public void setup() throws Exception {
    adhocBackend = new WireMockServer(WireMockConfiguration.options().port(backend1Port));
    scheduledBackend = new WireMockServer(WireMockConfiguration.options().port(backend2Port));

    adhocBackend.start();
    scheduledBackend.start();

    adhocBackend.stubFor(
        WireMock.post("/v1/statement")
            .willReturn(
                WireMock.aResponse()
                    .withBody(EXPECTED_RESPONSE1)
                    .withHeader("Content-Encoding", "plain")
                    .withStatus(200)));
    scheduledBackend.stubFor(
        WireMock.post("/v1/statement")
            .willReturn(
                WireMock.aResponse()
                    .withBody(EXPECTED_RESPONSE2)
                    .withHeader("Content-Encoding", "plain")
                    .withStatus(200)));
  }

  @Test
  public void testQueryDeliveryToMultipleRoutingGroups() throws Exception {
    // Start Gateway
    ProxyBackendConfiguration adhocBackendConfig =
        GatewayTestUtil.getProxyBackendConfiguration(
            "adhoc1", "adhoc", backend1Port, 22000 + (int) (Math.random() * 100));

    ProxyBackendConfiguration scheduledBackendConfig =
        GatewayTestUtil.getProxyBackendConfiguration(
            "scheduled1", "scheduled", backend2Port, 22100 + (int) (Math.random() * 100));

    String configPath =
        GatewayTestUtil.buildGatewayConfigPath(
            routerPort, Arrays.asList(adhocBackendConfig, scheduledBackendConfig));

    String[] args = {"server", configPath};
    GatewayLauncher.main(args);

    // Default request should be routed to adhoc backend
    OkHttpClient httpClient = new OkHttpClient();
    RequestBody requestBody = RequestBody.create(GatewayTestUtil.JSON, "SELECT 1");
    Request request1 =
        new Request.Builder()
            .url("http://localhost:" + routerPort + "/v1/statement")
            .post(requestBody)
            .build();
    Response response1 = httpClient.newCall(request1).execute();
    Assert.assertEquals(response1.body().string(), EXPECTED_RESPONSE1);

    // When X-Presto-Routing-Group header is set to a routing group, request should be routed to a
    // cluster under the routing group
    Request request2 =
        new Request.Builder()
            .url("http://localhost:" + routerPort + "/v1/statement")
            .post(requestBody)
            .addHeader("X-Presto-Routing-Group", "scheduled")
            .build();
    Response response2 = httpClient.newCall(request2).execute();
    Assert.assertEquals(response2.body().string(), EXPECTED_RESPONSE2);

    Request request3 =
        new Request.Builder()
            .url("http://localhost:" + routerPort + "/v1/statement")
            .post(requestBody)
            .addHeader("X-Presto-Routing-Group", "adhoc")
            .build();
    Response response3 = httpClient.newCall(request3).execute();
    Assert.assertEquals(response3.body().string(), EXPECTED_RESPONSE1);
  }

  @AfterClass(alwaysRun = true)
  public void cleanup() throws IOException {
    adhocBackend.stop();
    scheduledBackend.stop();
  }
}
