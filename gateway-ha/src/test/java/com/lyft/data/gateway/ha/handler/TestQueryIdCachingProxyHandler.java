package com.lyft.data.gateway.ha.handler;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.lyft.data.gateway.ha.HaGatewayLauncher;
import com.lyft.data.gateway.ha.HaGatewayTestUtils;
import com.lyft.data.proxyserver.wrapper.MultiReadHttpServletRequest;
import java.io.IOException;
import java.net.URISyntaxException;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TestQueryIdCachingProxyHandler {
  public static final String EXPECTED_RESPONSE = "{\"id\":\"20210108_184021_01888_vj6pb\","
      + "\"infoUri\":\"https://prestogateway/ui/query"
      + ".html?20210108_184021_01888_vj6pb\",\"nextUri\":\"https://prestogateway/v1/statement/executing/20210108_184021_01888_vj6pb"
      + "/yb0c9aa3a38386f9ed32769942de3a071429b48d2/0\",\"stats\":{\"state\":\"QUEUED\","
      + "\"queued\":true,\"scheduled\":false,\"nodes\":0,\"totalSplits\":0,\"queuedSplits\":0,"
      + "\"runningSplits\":0,\"completedSplits\":0,\"cpuTimeMillis\":0,\"wallTimeMillis\":0,"
      + "\"queuedTimeMillis\":2,\"elapsedTimeMillis\":39719,\"processedRows\":0,"
      + "\"processedBytes\":0,\"physicalInputBytes\":0,\"peakMemoryBytes\":0,\"spilledBytes\":0},"
      + "\"warnings\":[]}";

  public static final String backendPrestoServer = "presto1";
  int backendPort = 20000 + (int) (Math.random() * 1000);
  int routerPort = 21000 + (int) (Math.random() * 1000);

  private WireMockServer backend =
      new WireMockServer(WireMockConfiguration.options().port(backendPort));
  private final String gatewayHost = "prestogateway";

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
        backendPrestoServer, "http://" + backendPrestoServer + ":" + backendPort, "externalUrl", true, "adhoc", routerPort);
  }

  @Test
  public void testExtractQueryIdFromUrl() throws IOException {
    String[] paths = {
        "/ui/api/query/20200416_160256_03078_6b4yt",
        "/ui/api/query/20200416_160256_03078_6b4yt?bogus_fictional_param",
        "/ui/api/query?query_id=20200416_160256_03078_6b4yt"};
    for (String path : paths) {
      String queryId = QueryIdCachingProxyHandler.extractQueryIdIfPresent(path, null);
      assertEquals(queryId,"20200416_160256_03078_6b4yt");
    }
    String[] nonPaths = {
        "/ui/api/query/myOtherThing",
        "/ui/api/query/20200416_blah?bogus_fictional_param"};
    for (String path : nonPaths) {
      String queryId = QueryIdCachingProxyHandler.extractQueryIdIfPresent(path, null);
      assertNull(queryId);
    }
  }

  @Test
  public void testOverwriteHostHeader() throws IOException, URISyntaxException {
    MultiReadHttpServletRequest request = new MultiReadHttpServletRequest(new TestHttpServletRequest("http://" + gatewayHost + "/v1/statement"));
    QueryIdCachingProxyHandler.overwriteHostHeader(request, "http://" + backendPrestoServer + ":" + backendPort);
    Assert.assertEquals(request.getHeader("Host"), backendPrestoServer + ":" + backendPort);
    Assert.assertEquals(request.getHeader("X-Forwarded-Host"), gatewayHost);
  }
}
