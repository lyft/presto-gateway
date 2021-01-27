package com.lyft.data.gateway.ha.handler;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.io.IOException;
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
}
