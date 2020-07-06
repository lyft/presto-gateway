package com.lyft.data.gateway.ha.router;

import static com.lyft.data.gateway.ha.router.ResourceGroupsManager.GlobalPropertiesDetail;
import static com.lyft.data.gateway.ha.router.ResourceGroupsManager.ResourceGroupsDetail;
import static com.lyft.data.gateway.ha.router.ResourceGroupsManager.SelectorsDetail;

import com.lyft.data.gateway.ha.HaGatewayTestUtils;
import com.lyft.data.gateway.ha.config.DataStoreConfiguration;
import com.lyft.data.gateway.ha.persistence.JdbcConnectionManager;
import java.io.File;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Slf4j
@Test
public class TestResourceGroupManager {
  private ResourceGroupsManager resourceGroupManager;
  //  private static final Logger logger =
  // Logger.getLogger(TestResourceGroupManager.class.getName());

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

    resourceGroup.setResourceGroupId(0);
    resourceGroup.setName("admin");
    resourceGroup.setHardConcurrencyLimit(20);
    resourceGroup.setMaxQueued(200);
    resourceGroup.setJmxExport(true);
    resourceGroup.setSoftMemoryLimit("80%");

    ResourceGroupsDetail newResourceGroup = resourceGroupManager.createResourceGroup(resourceGroup);

    Assert.assertEquals(newResourceGroup, resourceGroup);
  }

  @Test(dependsOnMethods = {"testCreateResourceGroup"})
  public void testReadResourceGroup() {
    List<ResourceGroupsDetail> resourceGroups = resourceGroupManager.readResourceGroup();
    Assert.assertEquals(resourceGroups.size(), 1);

    Assert.assertEquals(resourceGroups.get(0).getResourceGroupId(), 0);
    Assert.assertEquals(resourceGroups.get(0).getName(), "admin");
    Assert.assertEquals(resourceGroups.get(0).getHardConcurrencyLimit(), 20);
    Assert.assertEquals(resourceGroups.get(0).getMaxQueued(), 200);
    Assert.assertEquals(resourceGroups.get(0).isJmxExport(), true);
    Assert.assertEquals(resourceGroups.get(0).getSoftMemoryLimit(), "80%");
  }

  @Test(dependsOnMethods = {"testReadResourceGroup"})
  public void testUpdateResourceGroup() {
    ResourceGroupsDetail resourceGroup = new ResourceGroupsDetail();
    resourceGroup.setResourceGroupId(0);
    resourceGroup.setName("admin");
    resourceGroup.setHardConcurrencyLimit(50);
    resourceGroup.setMaxQueued(50);
    resourceGroup.setJmxExport(false);
    resourceGroup.setSoftMemoryLimit("20%");

    ResourceGroupsDetail updated = resourceGroupManager.updateResourceGroup(resourceGroup);
    List<ResourceGroupsDetail> resourceGroups = resourceGroupManager.readResourceGroup();
    Assert.assertEquals(resourceGroups.size(), 1);
    Assert.assertEquals(updated, resourceGroup);

    /* Update resourceGroups that do not exist yet.
     *  In this case, new resourceGroups should be created. */
    resourceGroup.setResourceGroupId(1);
    resourceGroup.setName("localization-eng");
    resourceGroup.setHardConcurrencyLimit(50);
    resourceGroup.setMaxQueued(70);
    resourceGroup.setJmxExport(true);
    resourceGroup.setSoftMemoryLimit("20%");
    resourceGroup.setSoftConcurrencyLimit(20);
    resourceGroupManager.updateResourceGroup(resourceGroup);

    resourceGroup.setResourceGroupId(3);
    resourceGroup.setName("resource_group_3");
    resourceGroup.setHardConcurrencyLimit(10);
    resourceGroup.setMaxQueued(150);
    resourceGroup.setJmxExport(true);
    resourceGroup.setSoftMemoryLimit("60%");
    resourceGroup.setSoftConcurrencyLimit(40);
    resourceGroupManager.updateResourceGroup(resourceGroup);

    resourceGroups = resourceGroupManager.readResourceGroup();

    Assert.assertEquals(
        resourceGroups.size(), 3); // updated 2 non-existing groups, so count should be 3

    Assert.assertEquals(resourceGroups.get(0).getResourceGroupId(), 0);
    Assert.assertEquals(resourceGroups.get(0).getName(), "admin");
    Assert.assertEquals(resourceGroups.get(0).getHardConcurrencyLimit(), 50);
    Assert.assertEquals(resourceGroups.get(0).getMaxQueued(), 50);
    Assert.assertEquals(resourceGroups.get(0).isJmxExport(), false);
    Assert.assertEquals(resourceGroups.get(0).getSoftMemoryLimit(), "20%");

    Assert.assertEquals(resourceGroups.get(1).getResourceGroupId(), 1);
    Assert.assertEquals(resourceGroups.get(1).getName(), "localization-eng");
    Assert.assertEquals(resourceGroups.get(1).getHardConcurrencyLimit(), 50);
    Assert.assertEquals(resourceGroups.get(1).getMaxQueued(), 70);
    Assert.assertEquals(resourceGroups.get(1).isJmxExport(), true);
    Assert.assertEquals(resourceGroups.get(1).getSoftMemoryLimit(), "20%");
    Assert.assertEquals(resourceGroups.get(1).getSoftConcurrencyLimit(), 20);
  }

  @Test(dependsOnMethods = {"testUpdateResourceGroup"})
  public void testDeleteResourceGroup() {
    List<ResourceGroupsDetail> resourceGroups = resourceGroupManager.readResourceGroup();
    Assert.assertEquals(resourceGroups.size(), 3);
    Assert.assertEquals(resourceGroups.get(0).getName(), "admin");

    resourceGroupManager.deleteResourceGroup(resourceGroups.get(1).getResourceGroupId());
    resourceGroups = resourceGroupManager.readResourceGroup();

    Assert.assertEquals(resourceGroups.size(), 2);
    Assert.assertEquals(resourceGroups.get(0).getResourceGroupId(), 0);
    Assert.assertEquals(resourceGroups.get(1).getResourceGroupId(), 3);

    resourceGroupManager.deleteResourceGroup(resourceGroups.get(1).getResourceGroupId());
    resourceGroups = resourceGroupManager.readResourceGroup();

    Assert.assertEquals(resourceGroups.size(), 1);
    Assert.assertEquals(resourceGroups.get(0).getResourceGroupId(), 0);
  }

  @Test(dependsOnMethods = {"testCreateResourceGroup"})
  public void testCreateSelector() {
    SelectorsDetail selector = new SelectorsDetail();
    selector.setResourceGroupId(0);
    selector.setPriority(0);
    selector.setUserRegex("data-platform-admin");
    selector.setSourceRegex("admin");
    selector.setQueryType("query_type");
    selector.setClientTags("client_tag");
    selector.setSelectorResourceEstimate("estimate");

    SelectorsDetail newSelector = resourceGroupManager.createSelector(selector);

    Assert.assertEquals(newSelector, selector);
  }

  @Test(dependsOnMethods = {"testCreateSelector"})
  public void testReadSelector() {
    List<SelectorsDetail> selectors = resourceGroupManager.readSelector();

    Assert.assertEquals(selectors.size(), 1);
    Assert.assertEquals(selectors.get(0).getResourceGroupId(), 0);
    Assert.assertEquals(selectors.get(0).getPriority(), 0);
    Assert.assertEquals(selectors.get(0).getUserRegex(), "data-platform-admin");
    Assert.assertEquals(selectors.get(0).getSourceRegex(), "admin");
    Assert.assertEquals(selectors.get(0).getQueryType(), "query_type");
    Assert.assertEquals(selectors.get(0).getClientTags(), "client_tag");
    Assert.assertEquals(selectors.get(0).getSelectorResourceEstimate(), "estimate");
  }

  @Test(dependsOnMethods = {"testReadSelector"})
  public void testUpdateSelector() {
    SelectorsDetail selector = new SelectorsDetail();

    selector.setResourceGroupId(0);
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

  public void testCreateGlobalProperties() {
    GlobalPropertiesDetail globalPropertiesDetail = new GlobalPropertiesDetail();
    globalPropertiesDetail.setName("cpu_quota_period");
    globalPropertiesDetail.setValue("test_value");

    GlobalPropertiesDetail newGlobalProperties =
        resourceGroupManager.createGlobalProperty(globalPropertiesDetail);
    Assert.assertEquals(newGlobalProperties, globalPropertiesDetail);
  }

  @Test(dependsOnMethods = {"testCreateGlobalProperties"})
  public void testReadGlobalProperties() {
    List<GlobalPropertiesDetail> globalProperties = resourceGroupManager.readGlobalProperty();
    Assert.assertEquals(globalProperties.size(), 1);
  }

  @Test(dependsOnMethods = {"testReadGlobalProperties"})
  public void testUpdateGlobalProperties() {}

  @Test(dependsOnMethods = {"testUpdateGlobalProperties"})
  public void testDeleteGlobalProperties() {}

  public void testCreateExactMatchSourceSelectors() {}

  @Test(dependsOnMethods = {"testCreateExactMatchSourceSelectors"})
  public void testReadExactMatchSourceSelectors() {}

  @Test(dependsOnMethods = {"testReadExactMatchSourceSelectors"})
  public void testUpdateExactMatchSourceSelectors() {}

  @Test(dependsOnMethods = {"testUpdateExactMatchSourceSelectors"})
  public void testDeleteExactMatchSourceSelectors() {}

  @AfterClass(alwaysRun = true)
  public void cleanUp() {}
}
