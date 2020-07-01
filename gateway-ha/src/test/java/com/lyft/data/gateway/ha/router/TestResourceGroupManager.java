package com.lyft.data.gateway.ha.router;

import static com.lyft.data.gateway.ha.router.ResourceGroupsManager.ResourceGroupsDetail;
import static com.lyft.data.gateway.ha.router.ResourceGroupsManager.SelectorsDetail;

import com.lyft.data.gateway.ha.HaGatewayTestUtils;
import com.lyft.data.gateway.ha.config.DataStoreConfiguration;
import com.lyft.data.gateway.ha.persistence.JdbcConnectionManager;
import java.io.File;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test
public class TestResourceGroupManager {
  private HaResourceGroupsManager resourceGroupManager;

  @BeforeClass(alwaysRun = true)
  public void setUp() {
    File baseDir = new File(System.getProperty("java.io.tmpdir"));
    File tempH2DbDir = new File(baseDir, "h2db-" + System.currentTimeMillis());
    tempH2DbDir.deleteOnExit();
    String jdbcUrl = "jdbc:h2:" + tempH2DbDir.getAbsolutePath();
    HaGatewayTestUtils.seedRequiredData(
        new HaGatewayTestUtils.TestConfig("", tempH2DbDir.getAbsolutePath()));
    DataStoreConfiguration db = new DataStoreConfiguration(jdbcUrl, "sa", "sa", "org.h2.Driver");
    JdbcConnectionManager connectionManager = new JdbcConnectionManager(db);
    resourceGroupManager = new HaResourceGroupsManager(connectionManager);
  }

  public void testCreateResourceGroup() {
    ResourceGroupsDetail resourceGroup = new ResourceGroupsDetail();
    resourceGroup.setName("admin");
    resourceGroup.setHardConcurrencyLimit(20);
    resourceGroup.setMaxQueued(200);
    resourceGroup.setJmxExport(true);
    resourceGroup.setSoftMemoryLimit("80%");

    ResourceGroupsDetail updated = resourceGroupManager.createResourceGroup(resourceGroup);

    Assert.assertEquals(updated, resourceGroup);
  }

  @Test(dependsOnMethods = {"testCreateResourceGroup"})
  public void testReadResourceGroup() {
    List<ResourceGroupsDetail> resourceGroups = resourceGroupManager.readResourceGroup();
    Assert.assertEquals(resourceGroups.size(), 1);
  }

  @Test(dependsOnMethods = {"testReadResourceGroup"})
  public void testUpdateResourceGroup() {
    ResourceGroupsDetail resourceGroup = new ResourceGroupsDetail();
    resourceGroup.setName("admin");
    resourceGroup.setSoftMemoryLimit("20%");
    resourceGroup.setMaxQueued(50);
    resourceGroup.setJmxExport(false);
    resourceGroup.setHardConcurrencyLimit(50);

    ResourceGroupsDetail updated = resourceGroupManager.updateResourceGroup(resourceGroup);
    List<ResourceGroupsDetail> resourceGroups = resourceGroupManager.readResourceGroup();
    Assert.assertEquals(resourceGroups.size(), 1);
    Assert.assertEquals(updated, resourceGroup);

    /* Update resourceGroup that doesn't exist yet */
    resourceGroup.setName("localization-eng");
    resourceGroup.setSoftMemoryLimit("20%");
    resourceGroup.setMaxQueued(70);
    resourceGroup.setJmxExport(true);
    resourceGroup.setHardConcurrencyLimit(50);
    resourceGroup.setResourceGroupId(1);
    resourceGroup.setSoftConcurrencyLimit(20);

    resourceGroupManager.updateResourceGroup(resourceGroup);
    resourceGroups = resourceGroupManager.readResourceGroup();

    Assert.assertEquals(resourceGroups.size(), 2);
    Assert.assertEquals(resourceGroups.get(0).getMaxQueued(), 50);
    Assert.assertEquals(resourceGroups.get(1).getMaxQueued(), 70);
  }

  @Test(dependsOnMethods = {"testUpdateResourceGroup"})
  public void testDeleteResourceGroup() {
    List<ResourceGroupsDetail> resourceGroups = resourceGroupManager.readResourceGroup();
    Assert.assertEquals(resourceGroups.size(), 2);
    Assert.assertEquals(resourceGroups.get(0).getName(), "admin");
    resourceGroupManager.deleteResourceGroup(resourceGroups.get(1).getResourceGroupId());
    resourceGroups = resourceGroupManager.readResourceGroup();
    Assert.assertEquals(resourceGroups.size(), 1);
  }

  @Test(dependsOnMethods = {"testCreateResourceGroup"})
  public void testCreateSelector() {
    SelectorsDetail selector = new SelectorsDetail();
    selector.setPriority(0);
    selector.setUserRegex("data-platform-admin");
    selector.setSourceRegex("admin");
    selector.setQueryType("query_type");
    selector.setClientTags("client_tag");
    selector.setSelectorResourceEstimate("estimate");

    SelectorsDetail updated = resourceGroupManager.createSelector(selector);

    Assert.assertEquals(updated, selector);
  }

  @Test(dependsOnMethods = {"testCreateSelector"})
  public void testReadSelector() {
    List<SelectorsDetail> selectors = resourceGroupManager.readSelector();
    Assert.assertEquals(selectors.size(), 1);
  }

  @Test(dependsOnMethods = {"testReadSelector"})
  public void testUpdateSelector() {
    SelectorsDetail selector = new SelectorsDetail();
    selector.setPriority(0);
    selector.setUserRegex("data-platform-admin_updated");
    selector.setSourceRegex("admin_updated");
    selector.setQueryType("query_type_updated");
    selector.setClientTags("client_tag_updated");
    selector.setSelectorResourceEstimate("estimate_updated");

    SelectorsDetail updated = resourceGroupManager.updateSelector(selector);
    List<SelectorsDetail> selectors = resourceGroupManager.readSelector();
    Assert.assertEquals(selectors.size(), 1);
    Assert.assertEquals(updated, selector);
  }

  @Test(dependsOnMethods = {"testUpdateSelector"})
  public void testDeleteSelector() {
    List<SelectorsDetail> selectors = resourceGroupManager.readSelector();
    Assert.assertEquals(selectors.size(), 1);
    Assert.assertEquals(selectors.get(0).getSourceRegex(), "admin_updated");
    resourceGroupManager.deleteSelector(selectors.get(0).getResourceGroupId());
    selectors = resourceGroupManager.readSelector();
    Assert.assertEquals(selectors.size(), 0);
  }

  //  public void testReadGlobalProperties(){
  //    List<GlobalPropertyDetail> globalProperties =
  // resourceGroupManager.readGlobalProperty();
  //    Assert.assertEquals(globalProperties.size(), 1);
  //  }
  //
  //  public void testUpdateGlobalProperty(){
  //
  //  }

  @AfterClass(alwaysRun = true)
  public void cleanUp() {}
}
