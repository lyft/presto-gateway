package com.lyft.data.gateway.ha.router;

import static com.lyft.data.gateway.ha.router.ResourceGroupsManager.ResourceGroupsDetail;

import com.lyft.data.gateway.ha.HaGatewayTestUtils;
import com.lyft.data.gateway.ha.config.DataStoreConfiguration;
import com.lyft.data.gateway.ha.persistence.JdbcConnectionManager;
import java.io.File;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Slf4j
@Test
public class TestSpecificDbResourceGroupsManager extends TestResourceGroupsManager {
  private String specificDb;

  @BeforeClass(alwaysRun = true)
  @Override
  public void setUp() {
    specificDb = "h2db-" + System.currentTimeMillis();
    File baseDir = new File(System.getProperty("java.io.tmpdir"));
    File tempH2DbDir = new File(baseDir, specificDb);
    tempH2DbDir.deleteOnExit();
    String jdbcUrl = "jdbc:h2:" + tempH2DbDir.getAbsolutePath();
    HaGatewayTestUtils.seedRequiredData(
            new HaGatewayTestUtils.TestConfig("", tempH2DbDir.getAbsolutePath()));
    DataStoreConfiguration db = new DataStoreConfiguration(jdbcUrl, "sa",
            "sa", "org.h2.Driver");
    JdbcConnectionManager connectionManager = new JdbcConnectionManager(db);
    super.resourceGroupManager = new HaResourceGroupsManager(connectionManager);
  }

  private void createResourceGroup() {
    ResourceGroupsDetail resourceGroup = new ResourceGroupsDetail();

    resourceGroup.setResourceGroupId(1L);
    resourceGroup.setName("admin2");
    resourceGroup.setHardConcurrencyLimit(20);
    resourceGroup.setMaxQueued(200);
    resourceGroup.setJmxExport(true);
    resourceGroup.setSoftMemoryLimit("80%");

    ResourceGroupsDetail newResourceGroup = resourceGroupManager.createResourceGroup(resourceGroup,
            specificDb);
  }

  @Test(expectedExceptions = Exception.class)
  public void testReadSpecificDbResourceGroupCauseException() {
    List<ResourceGroupsDetail> resourceGroups = resourceGroupManager.readAllResourceGroups("abcd");
  }

  public void testReadSpecificDbResourceGroup() {
    this.createResourceGroup();
    List<ResourceGroupsDetail> resourceGroups = resourceGroupManager
            .readAllResourceGroups(specificDb);
    Assert.assertNotNull(resourceGroups);
    resourceGroupManager.deleteResourceGroup(1,specificDb);
  }

  public void testReadSpecificDbSelector() {
    this.createResourceGroup();
    ResourceGroupsManager.SelectorsDetail selector = new ResourceGroupsManager.SelectorsDetail();
    selector.setResourceGroupId(1L);
    selector.setPriority(0L);
    selector.setUserRegex("data-platform-admin");
    selector.setSourceRegex("admin2");
    selector.setQueryType("query_type");
    selector.setClientTags("client_tag");
    selector.setSelectorResourceEstimate("estimate");

    ResourceGroupsManager.SelectorsDetail newSelector = resourceGroupManager
            .createSelector(selector, specificDb);

    Assert.assertEquals(newSelector, selector);
    resourceGroupManager
            .deleteSelector(selector, specificDb);
    resourceGroupManager.deleteResourceGroup(1,specificDb);
  }
}
