package com.lyft.data.gateway.ha.router;

import static com.lyft.data.gateway.ha.router.RoutingGroupSelector.ROUTING_GROUP_HEADER;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Scanner;
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
    String rulesConfigPath = "src/test/resources/rules/routing_rules.yml";
    RoutingGroupSelector routingGroupSelector =
        RoutingGroupSelector.byRoutingRulesEngine(rulesConfigPath);

    HttpServletRequest mockRequest = mock(HttpServletRequest.class);

    when(mockRequest.getHeader(TRINO_SOURCE_HEADER)).thenReturn("airflow");
    Assert.assertEquals(
        routingGroupSelector.findRoutingGroup(mockRequest), "etl");
  }

  public void testByRoutingRulesEngineSpecialLabel() {
    String rulesConfigPath = "src/test/resources/rules/routing_rules.yml";
    RoutingGroupSelector routingGroupSelector =
        RoutingGroupSelector.byRoutingRulesEngine(rulesConfigPath);

    HttpServletRequest mockRequest = mock(HttpServletRequest.class);

    when(mockRequest.getHeader(TRINO_SOURCE_HEADER)).thenReturn("airflow");
    when(mockRequest.getHeader(TRINO_CLIENT_TAGS_HEADER)).thenReturn(
        "email=test@example.com,label=special");
    Assert.assertEquals(
        routingGroupSelector.findRoutingGroup(mockRequest), "etl-special");
  }

  public void testByRoutingRulesEngineNoMatch() {
    String rulesConfigPath = "src/test/resources/rules/routing_rules.yml";
    RoutingGroupSelector routingGroupSelector =
        RoutingGroupSelector.byRoutingRulesEngine(rulesConfigPath);

    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    // even though special label is present, query is not from airflow.
    // should return no match
    when(mockRequest.getHeader(TRINO_CLIENT_TAGS_HEADER)).thenReturn(
        "email=test@example.com,label=special");
    Assert.assertEquals(
        routingGroupSelector.findRoutingGroup(mockRequest), null);
  }

  public void testByRoutingRulesEngineCompositeRules() {
    String rulesConfigPath = "src/test/resources/rules/routing_rules_composite.yml";
    RoutingGroupSelector routingGroupSelector =
        RoutingGroupSelector.byRoutingRulesEngine(rulesConfigPath);

    HttpServletRequest mockRequest = mock(HttpServletRequest.class);

    when(mockRequest.getHeader(TRINO_SOURCE_HEADER)).thenReturn("airflow");
    Assert.assertEquals(
        routingGroupSelector.findRoutingGroup(mockRequest), "etl");
  }

  public void testByRoutingRulesEngineCompositeRulesSpecialLabel() {
    String rulesConfigPath = "src/test/resources/rules/routing_rules_composite.yml";
    RoutingGroupSelector routingGroupSelector =
        RoutingGroupSelector.byRoutingRulesEngine(rulesConfigPath);

    HttpServletRequest mockRequest = mock(HttpServletRequest.class);

    when(mockRequest.getHeader(TRINO_SOURCE_HEADER)).thenReturn("airflow");
    when(mockRequest.getHeader(TRINO_CLIENT_TAGS_HEADER)).thenReturn(
        "email=test@example.com,label=special");
    Assert.assertEquals(
        routingGroupSelector.findRoutingGroup(mockRequest), "etl-special");
  }

  public void testByRoutingRulesEngineCompositeRulesNoMatch() {
    String rulesConfigPath = "src/test/resources/rules/routing_rules_composite.yml";
    RoutingGroupSelector routingGroupSelector =
        RoutingGroupSelector.byRoutingRulesEngine(rulesConfigPath);

    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    // even though special label is present, query is not from airflow.
    // should return no match
    when(mockRequest.getHeader(TRINO_CLIENT_TAGS_HEADER)).thenReturn(
        "email=test@example.com,label=special");
    Assert.assertEquals(
        routingGroupSelector.findRoutingGroup(mockRequest), null);
  }

  public void testByRoutingRulesEngineFileChange() {
  }

}
