package com.lyft.data.gateway.ha.router;

import static com.lyft.data.gateway.ha.router.RoutingGroupSelector.ROUTING_GROUP_HEADER;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileReader;
import javax.servlet.http.HttpServletRequest;
import org.jeasy.rules.api.Rule;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.core.RuleBuilder;
import org.jeasy.rules.mvel.MVELRuleFactory;
import org.jeasy.rules.support.reader.JsonRuleDefinitionReader;
import org.jeasy.rules.support.reader.YamlRuleDefinitionReader;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test
public class TestRoutingGroupSelector {
  public static final String TRINO_SOURCE_HEADER = "X-Trino-Source";

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

  public void testByRoutingRulesEngine() {
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);

    when(mockRequest.getHeader(TRINO_SOURCE_HEADER)).thenReturn("airflow");

    Rule airflowRule = new RuleBuilder()
        .name("airflow rule")
        .description("if airflow route to etl cluster")
        .when(facts -> ((HttpServletRequest) facts.get("request")).getHeader(
              TRINO_SOURCE_HEADER).equals("airflow"))
        .then(facts -> facts.put("routingGroup", "etl"))
        .build();

    Rules rules = new Rules(airflowRule);
    Assert.assertEquals(
        RoutingGroupSelector.byRoutingRulesEngine(rules).findRoutingGroup(mockRequest),
        "etl");

  }
}
