package com.lyft.data.gateway.ha.router;

import static com.lyft.data.gateway.ha.router.RoutingGroupSelector.ROUTING_GROUP_HEADER;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;
import javax.servlet.http.HttpServletRequest;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
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

  @DataProvider(name = "routingRuleConfigFiles")
  public Object[][] provideData() {
    String rulesDir = "src/test/resources/rules/";

    return new Object[][] {
      { rulesDir + "routing_rules_atomic.yml" },
      { rulesDir + "routing_rules_composite.yml" },
      { rulesDir + "routing_rules_priorities.yml" },
      { rulesDir + "routing_rules_if_statements.yml" }
    };
  }

  @Test(dataProvider = "routingRuleConfigFiles")
  public void testByRoutingRulesEngine(String rulesConfigPath) {
    RoutingGroupSelector routingGroupSelector =
        RoutingGroupSelector.byRoutingRulesEngine(rulesConfigPath);

    HttpServletRequest mockRequest = mock(HttpServletRequest.class);

    when(mockRequest.getHeader(TRINO_SOURCE_HEADER)).thenReturn("airflow");
    Assert.assertEquals(
        routingGroupSelector.findRoutingGroup(mockRequest), "etl");
  }

  @Test(dataProvider = "routingRuleConfigFiles")
  public void testByRoutingRulesEngineSpecialLabel(String rulesConfigPath) {
    RoutingGroupSelector routingGroupSelector =
        RoutingGroupSelector.byRoutingRulesEngine(rulesConfigPath);

    HttpServletRequest mockRequest = mock(HttpServletRequest.class);

    when(mockRequest.getHeader(TRINO_SOURCE_HEADER)).thenReturn("airflow");
    when(mockRequest.getHeader(TRINO_CLIENT_TAGS_HEADER)).thenReturn(
        "email=test@example.com,label=special");
    Assert.assertEquals(
        routingGroupSelector.findRoutingGroup(mockRequest), "etl-special");
  }

  @Test(dataProvider = "routingRuleConfigFiles")
  public void testByRoutingRulesEngineNoMatch(String rulesConfigPath) {
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

  public void testByRoutingRulesEngineFileChange() throws Exception {
    File file = File.createTempFile("routing_rules", ".yml");

    FileWriter fw = new FileWriter(file);
    fw.write(
        "---\n"
        + "name: \"airflow\"\n"
        + "description: \"if query from airflow, route to etl group\"\n"
        + "condition: \"request.getHeader(\\\"X-Trino-Source\\\") == \\\"airflow\\\"\"\n"
        + "actions:\n"
        + "  - \"result.put(\\\"routingGroup\\\", \\\"etl\\\")\"");
    fw.close();

    RoutingGroupSelector routingGroupSelector =
        RoutingGroupSelector.byRoutingRulesEngine(file.getPath());

    HttpServletRequest mockRequest = mock(HttpServletRequest.class);

    when(mockRequest.getHeader(TRINO_SOURCE_HEADER)).thenReturn("airflow");
    Assert.assertEquals(
        routingGroupSelector.findRoutingGroup(mockRequest), "etl");

    fw = new FileWriter(file);
    fw.write(
        "---\n"
        + "name: \"airflow\"\n"
        + "description: \"if query from airflow, route to etl group\"\n"
        + "condition: \"request.getHeader(\\\"X-Trino-Source\\\") == \\\"airflow\\\"\"\n"
        + "actions:\n"
        + "  - \"result.put(\\\"routingGroup\\\", \\\"etl2\\\")\""); // change from etl to etl2
    fw.close();

    when(mockRequest.getHeader(TRINO_SOURCE_HEADER)).thenReturn("airflow");
    Assert.assertEquals(
        routingGroupSelector.findRoutingGroup(mockRequest), "etl2");
    file.deleteOnExit();
  }
}
