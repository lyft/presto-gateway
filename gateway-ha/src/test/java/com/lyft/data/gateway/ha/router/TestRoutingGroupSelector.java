package com.lyft.data.gateway.ha.router;

import static com.lyft.data.gateway.ha.router.RoutingGroupSelector.ROUTING_GROUP_HEADER;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test
public class TestRoutingGroupSelector {
  public void testByRoutingGroupHeader() {
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);

    // If the header is present the routing group is the value of that header.
    when(mockRequest.getHeader(ROUTING_GROUP_HEADER)).thenReturn("batch_backend");
    Assert.assertEquals(
        RoutingGroupSelector.byRoutingGroupHeader().findRoutingGroup(mockRequest), "batch_backend");

    // If the header is not present just return null.
    when(mockRequest.getHeader(ROUTING_GROUP_HEADER)).thenReturn(null);
    Assert.assertNull(RoutingGroupSelector.byRoutingGroupHeader().findRoutingGroup(mockRequest));
  }
}
