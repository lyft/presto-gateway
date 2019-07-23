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

public class TestPrestoGatewaySingleBackend {
  public static final String EXPECTED_RESPONSE = "{\"id\":\"testId\"}";
  int backendPort = 20000 + (int) (Math.random() * 1000);
  int routerPort = 21000 + (int) (Math.random() * 1000);

  private WireMockServer backend =
      new WireMockServer(WireMockConfiguration.options().port(backendPort));

  @BeforeClass(alwaysRun = true)
  public void setup() throws Exception {
    backend.start();
    backend.stubFor(
        WireMock.post("/v1/statement")
            .willReturn(
                WireMock.aResponse()
                    .withBody(EXPECTED_RESPONSE)
                    .withHeader("Content-Encoding", "plain")
                    .withStatus(200)));

    // Start Gateway
    ProxyBackendConfiguration proxyBackendConfiguration =
        GatewayTestUtil.getProxyBackendConfiguration(
            "adhoc1", "adhoc", backendPort, 22000 + (int) (Math.random() * 1000));
    String configPath =
        GatewayTestUtil.buildGatewayConfigPath(
            routerPort, Arrays.asList(proxyBackendConfiguration));
    String[] args = {"server", configPath};
    GatewayLauncher.main(args);
  }

  @Test
  public void testRequestDelivery() throws Exception {
    OkHttpClient httpClient = new OkHttpClient();
    RequestBody requestBody = RequestBody.create(GatewayTestUtil.JSON, "SELECT 1");
    Request request =
        new Request.Builder()
            .url("http://localhost:" + routerPort + "/v1/statement")
            .post(requestBody)
            .build();
    Response response = httpClient.newCall(request).execute();
    Assert.assertEquals(EXPECTED_RESPONSE, response.body().string());
  }

  @AfterClass(alwaysRun = true)
  public void cleanup() throws IOException {
    backend.stop();
  }
}
