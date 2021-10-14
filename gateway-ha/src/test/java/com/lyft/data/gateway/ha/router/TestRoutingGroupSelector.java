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
import org.jeasy.rules.support.reader.YamlRuleDefinitionReader;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test
public class TestRoutingGroupSelector {
  public static final String TRINO_SOURCE_HEADER = "X-Trino-Source";
  public static final String TRINO_CLIENT_TAGS_HEADER = "X-Trino-Client-Tags";

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

    // query from airflow goes to etl
    when(mockRequest.getHeader(TRINO_SOURCE_HEADER)).thenReturn("airflow");
    Rule airflowRule = new RuleBuilder()
        .name("airflow rule")
        .description("if query from airflow, route to etl group")
        .when(facts -> ((HttpServletRequest) facts.get("request")).getHeader(
              TRINO_SOURCE_HEADER).equals("airflow"))
        .then(facts -> facts.put("routingGroup", "etl"))
        .build();

    Rules rules = new Rules(airflowRule);
    Assert.assertEquals(
        RoutingGroupSelector.byRoutingRulesEngine(rules).findRoutingGroup(mockRequest),
        "etl");

  }

  public void testByRoutingRulesEngine_FromFile() throws Exception {
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    Rules rules = new Rules();

    MVELRuleFactory ruleFactory = new MVELRuleFactory(new YamlRuleDefinitionReader());
    rules = ruleFactory.createRules(
        new FileReader("src/test/resources/rules/routing_rules.yml"));

    // query from airflow goes to etl
    when(mockRequest.getHeader(TRINO_SOURCE_HEADER)).thenReturn("airflow");
    Assert.assertEquals(
        RoutingGroupSelector.byRoutingRulesEngine(rules).findRoutingGroup(mockRequest),
        "etl");

    // query from airflow with label coco goes to etl-critical
    when(mockRequest.getHeader(TRINO_CLIENT_TAGS_HEADER)).thenReturn(
        "email=person@example.com,label=special");
    Assert.assertEquals(
        RoutingGroupSelector.byRoutingRulesEngine(rules).findRoutingGroup(mockRequest),
        "etl-special");

    // query from mode goes to scheduled
    when(mockRequest.getHeader(TRINO_SOURCE_HEADER)).thenReturn("mode");
    when(mockRequest.getHeader(TRINO_CLIENT_TAGS_HEADER)).thenReturn(null); // reset client tags
    Assert.assertEquals(
        RoutingGroupSelector.byRoutingRulesEngine(rules).findRoutingGroup(mockRequest),
        "scheduled");

    // if no rules matched, should return null
    when(mockRequest.getHeader(TRINO_SOURCE_HEADER)).thenReturn("unknown");
    Assert.assertNull(
        RoutingGroupSelector.byRoutingRulesEngine(rules).findRoutingGroup(mockRequest));

  }
}
