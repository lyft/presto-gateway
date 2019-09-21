package com.lyft.data.gateway.ha;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TestGatewayHaSingleBackend {
  public static final String EXPECTED_RESPONSE = "{\"id\":\"testId\"}";
  int backendPort = 20000 + (int) (Math.random() * 1000);
  int routerPort = 21000 + (int) (Math.random() * 1000);

  private WireMockServer backend =
      new WireMockServer(WireMockConfiguration.options().port(backendPort));
  private final OkHttpClient httpClient = new OkHttpClient();

  @BeforeClass(alwaysRun = true)
  public void setup() throws Exception {
    HaGatewayTestUtils.prepareMockBackend(backend, "/v1/statement", EXPECTED_RESPONSE);

    // seed database
    HaGatewayTestUtils.TestConfig testConfig =
        HaGatewayTestUtils.buildGatewayConfigAndSeedDb(routerPort);
    // Start Gateway
    String[] args = {"server", testConfig.getConfigFilePath()};
    HaGatewayLauncher.main(args);
    // Now populate the backend
    HaGatewayTestUtils.setUpBackend(
        "presto1", "http://localhost:" + backendPort, true, "adhoc", routerPort);
  }

  @Test
  public void testRequestDelivery() throws Exception {
    RequestBody requestBody =
        RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "SELECT 1");
    Request request =
        new Request.Builder()
            .url("http://localhost:" + routerPort + "/v1/statement")
            .post(requestBody)
            .build();
    Response response = httpClient.newCall(request).execute();
    Assert.assertEquals(EXPECTED_RESPONSE, response.body().string());
  }

  @AfterClass(alwaysRun = true)
  public void cleanup() {
    backend.stop();
  }
}
