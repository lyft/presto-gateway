package com.lyft.data.gateway.ha.handler;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestQueryIdCachingProxyHandler {

  @Test
  public void testExtractQueryIdFromUrl() throws IOException {
    String[] paths = {
        "/ui/api/query/20200416_160256_03078_6b4yt",
        "/ui/api/query/20200416_160256_03078_6b4yt?bogus_fictional_param",
        "/ui/api/query?query_id=20200416_160256_03078_6b4yt",
        "/ui/api/query.html?20200416_160256_03078_6b4yt"};
    for (String path : paths) {
      String queryId = QueryIdCachingProxyHandler.extractQueryIdIfPresent(path, null);
      assertEquals(queryId, "20200416_160256_03078_6b4yt");
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
  public void testForwardedHostHeaderOnProxyRequest() throws IOException {
    String backendServer = "prestocluster";
    String backendPort = "80";
    HttpServletRequest mockServletRequest = Mockito.mock(HttpServletRequest.class);
    Mockito.when(mockServletRequest.getHeader("proxytarget")).thenReturn(String.format("http://%s"
        + ":%s", backendServer, backendPort));
    HttpClient httpClient = new HttpClient();
    Request proxyRequest = httpClient.newRequest("http://localhost:80");
    QueryIdCachingProxyHandler.setForwardedHostHeaderOnProxyRequest(mockServletRequest,
        proxyRequest);
    Assert.assertEquals(proxyRequest.getHeaders().get("Host"), String.format("%s:%s",
        backendServer, backendPort));
  }

}
