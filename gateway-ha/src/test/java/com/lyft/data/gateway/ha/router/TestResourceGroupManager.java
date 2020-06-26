package com.lyft.data.gateway.ha.router;

import static com.lyft.data.gateway.ha.router.PrestoResourceManager.ResourceGroupDetail;

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
  private ResourceGroupManager resourceGroupManager;

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
    resourceGroupManager = new ResourceGroupManager(connectionManager);
  }

  public void testCreateResourceGroup() {
    ResourceGroupDetail resourceGroup = new ResourceGroupDetail();
    resourceGroup.setName("admin");
    resourceGroup.setHardConcurrencyLimit(20);
    resourceGroup.setMaxQueued(200);
    resourceGroup.setJmxExport(true);
    resourceGroup.setSoftMemoryLimit("80%");

    ResourceGroupDetail updated = resourceGroupManager.createResourceGroup(resourceGroup);

    Assert.assertEquals(updated, resourceGroup);
  }

  @Test(dependsOnMethods = {"testCreateResourceGroup"})
  public void testReadResourceGroup() {
    List<ResourceGroupDetail> resourceGroupList = resourceGroupManager.readResourceGroup();
    Assert.assertEquals(resourceGroupList.size(), 1);
  }

  @Test(dependsOnMethods = {"testReadResourceGroup"})
  public void testUpdateResourceGroup() {
    ResourceGroupDetail resourceGroup = new ResourceGroupDetail();
    resourceGroup.setName("admin");
    resourceGroup.setSoftMemoryLimit("20%");
    resourceGroup.setMaxQueued(50);
    resourceGroup.setJmxExport(false);
    resourceGroup.setHardConcurrencyLimit(50);

    ResourceGroupDetail updated = resourceGroupManager.updateResourceGroup(resourceGroup);
    List<ResourceGroupDetail> resourceGroups = resourceGroupManager.readResourceGroup();
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
    List<ResourceGroupDetail> resourceGroupList = resourceGroupManager.readResourceGroup();
    Assert.assertEquals(resourceGroupList.size(), 2);
    Assert.assertEquals(resourceGroupList.get(0).getName(), "admin");
    resourceGroupManager.deleteResourceGroup(resourceGroupList.get(1).getResourceGroupId());
    resourceGroupList = resourceGroupManager.readResourceGroup();
    Assert.assertEquals(resourceGroupList.size(), 1);
  }

  @AfterClass(alwaysRun = true)
  public void cleanUp() {}
}
