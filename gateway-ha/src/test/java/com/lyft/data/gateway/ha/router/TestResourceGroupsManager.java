package com.lyft.data.gateway.ha.router;

import static com.lyft.data.gateway.ha.router.ResourceGroupsManager.ExactSelectorsDetail;
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
public class TestResourceGroupsManager {
  public ResourceGroupsManager resourceGroupManager;

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

  @Test
  public void testCreateResourceGroup() {
    ResourceGroupsDetail resourceGroup = new ResourceGroupsDetail();

    resourceGroup.setResourceGroupId(0L);
    resourceGroup.setName("admin");
    resourceGroup.setHardConcurrencyLimit(20);
    resourceGroup.setMaxQueued(200);
    resourceGroup.setJmxExport(true);
    resourceGroup.setSoftMemoryLimit("80%");

    ResourceGroupsDetail newResourceGroup = resourceGroupManager.createResourceGroup(resourceGroup,
            null);

    Assert.assertEquals(newResourceGroup, resourceGroup);
  }

  @Test(dependsOnMethods = {"testCreateResourceGroup"})
  public void testReadResourceGroup() {
    List<ResourceGroupsDetail> resourceGroups = resourceGroupManager.readAllResourceGroups(null);
    Assert.assertEquals(resourceGroups.size(), 1);

    Assert.assertEquals(resourceGroups.get(0).getResourceGroupId(), 0L);
    Assert.assertEquals(resourceGroups.get(0).getName(), "admin");
    Assert.assertEquals(resourceGroups.get(0).getHardConcurrencyLimit(), 20);
    Assert.assertEquals(resourceGroups.get(0).getMaxQueued(), 200);
    Assert.assertEquals(resourceGroups.get(0).getJmxExport(), Boolean.TRUE);
    Assert.assertEquals(resourceGroups.get(0).getSoftMemoryLimit(), "80%");
  }

  @Test(dependsOnMethods = {"testReadResourceGroup"})
  public void testUpdateResourceGroup() {
    ResourceGroupsDetail resourceGroup = new ResourceGroupsDetail();
    resourceGroup.setResourceGroupId(0L);
    resourceGroup.setName("admin");
    resourceGroup.setHardConcurrencyLimit(50);
    resourceGroup.setMaxQueued(50);
    resourceGroup.setJmxExport(false);
    resourceGroup.setSoftMemoryLimit("20%");

    ResourceGroupsDetail updated = resourceGroupManager.updateResourceGroup(resourceGroup, null);
    List<ResourceGroupsDetail> resourceGroups = resourceGroupManager.readAllResourceGroups(null);
    Assert.assertEquals(resourceGroups.size(), 1);
    Assert.assertEquals(updated, resourceGroup);

    /* Update resourceGroups that do not exist yet.
     *  In this case, new resourceGroups should be created. */
    resourceGroup.setResourceGroupId(1L);
    resourceGroup.setName("localization-eng");
    resourceGroup.setHardConcurrencyLimit(50);
    resourceGroup.setMaxQueued(70);
    resourceGroup.setJmxExport(true);
    resourceGroup.setSoftMemoryLimit("20%");
    resourceGroup.setSoftConcurrencyLimit(20);
    resourceGroupManager.updateResourceGroup(resourceGroup, null);

    resourceGroup.setResourceGroupId(3L);
    resourceGroup.setName("resource_group_3");
    resourceGroup.setHardConcurrencyLimit(10);
    resourceGroup.setMaxQueued(150);
    resourceGroup.setJmxExport(true);
    resourceGroup.setSoftMemoryLimit("60%");
    resourceGroup.setSoftConcurrencyLimit(40);
    resourceGroupManager.updateResourceGroup(resourceGroup, null);

    resourceGroups = resourceGroupManager.readAllResourceGroups(null);

    Assert.assertEquals(
        resourceGroups.size(), 3); // updated 2 non-existing groups, so count should be 3

    Assert.assertEquals(resourceGroups.get(0).getResourceGroupId(), 0L);
    Assert.assertEquals(resourceGroups.get(0).getName(), "admin");
    Assert.assertEquals(resourceGroups.get(0).getHardConcurrencyLimit(), 50);
    Assert.assertEquals(resourceGroups.get(0).getMaxQueued(), 50);
    Assert.assertEquals(resourceGroups.get(0).getJmxExport(), Boolean.FALSE);
    Assert.assertEquals(resourceGroups.get(0).getSoftMemoryLimit(), "20%");

    Assert.assertEquals(resourceGroups.get(1).getResourceGroupId(), 1L);
    Assert.assertEquals(resourceGroups.get(1).getName(), "localization-eng");
    Assert.assertEquals(resourceGroups.get(1).getHardConcurrencyLimit(), 50);
    Assert.assertEquals(resourceGroups.get(1).getMaxQueued(), 70);
    Assert.assertEquals(resourceGroups.get(1).getJmxExport(), Boolean.TRUE);
    Assert.assertEquals(resourceGroups.get(1).getSoftMemoryLimit(), "20%");
    Assert.assertEquals(resourceGroups.get(1).getSoftConcurrencyLimit(), Integer.valueOf(20));
  }

  @Test(dependsOnMethods = {"testUpdateResourceGroup"})
  public void testDeleteResourceGroup() {
    List<ResourceGroupsDetail> resourceGroups = resourceGroupManager.readAllResourceGroups(null);
    Assert.assertEquals(resourceGroups.size(), 3);

    Assert.assertEquals(resourceGroups.get(0).getResourceGroupId(), 0L);
    Assert.assertEquals(resourceGroups.get(1).getResourceGroupId(), 1L);
    Assert.assertEquals(resourceGroups.get(2).getResourceGroupId(), 3L);

    resourceGroupManager.deleteResourceGroup(resourceGroups.get(1).getResourceGroupId(), null);
    resourceGroups = resourceGroupManager.readAllResourceGroups(null);

    Assert.assertEquals(resourceGroups.size(), 2);
    Assert.assertEquals(resourceGroups.get(0).getResourceGroupId(), 0L);
    Assert.assertEquals(resourceGroups.get(1).getResourceGroupId(), 3L);
  }

  @Test(dependsOnMethods = {"testDeleteResourceGroup"})
  public void testCreateSelector() {
    SelectorsDetail selector = new SelectorsDetail();
    selector.setResourceGroupId(0L);
    selector.setPriority(0L);
    selector.setUserRegex("data-platform-admin");
    selector.setSourceRegex("admin");
    selector.setQueryType("query_type");
    selector.setClientTags("client_tag");
    selector.setSelectorResourceEstimate("estimate");

    SelectorsDetail newSelector = resourceGroupManager.createSelector(selector, null);

    Assert.assertEquals(newSelector, selector);
  }

  @Test(dependsOnMethods = {"testCreateSelector"})
  public void testReadSelector() {
    List<SelectorsDetail> selectors = resourceGroupManager.readAllSelectors(null);

    Assert.assertEquals(selectors.size(), 1);
    Assert.assertEquals(selectors.get(0).getResourceGroupId(), 0L);
    Assert.assertEquals(selectors.get(0).getPriority(), 0L);
    Assert.assertEquals(selectors.get(0).getUserRegex(), "data-platform-admin");
    Assert.assertEquals(selectors.get(0).getSourceRegex(), "admin");
    Assert.assertEquals(selectors.get(0).getQueryType(), "query_type");
    Assert.assertEquals(selectors.get(0).getClientTags(), "client_tag");
    Assert.assertEquals(selectors.get(0).getSelectorResourceEstimate(), "estimate");
  }

  @Test(dependsOnMethods = {"testReadSelector"})
  public void testUpdateSelector() {
    SelectorsDetail selector = new SelectorsDetail();

    selector.setResourceGroupId(0L);
    selector.setPriority(0L);
    selector.setUserRegex("data-platform-admin_updated");
    selector.setSourceRegex("admin_updated");
    selector.setQueryType("query_type_updated");
    selector.setClientTags("client_tag_updated");
    selector.setSelectorResourceEstimate("estimate_updated");

    List<SelectorsDetail> selectors = resourceGroupManager.readAllSelectors(null);
    SelectorsDetail updated = resourceGroupManager.updateSelector(selectors.get(0), selector, null);
    selectors = resourceGroupManager.readAllSelectors(null);

    Assert.assertEquals(selectors.size(), 1);
    Assert.assertEquals(updated, selectors.get(0));

    /* Update selectors that do not exist yet.
     *  In this case, a new selector should be created. */
    selector.setResourceGroupId(3L);
    selector.setPriority(10L);
    selector.setUserRegex("localization-eng.user_${USER}");
    selector.setSourceRegex("mode-scheduled");
    selector.setQueryType(null);
    selector.setClientTags(null);
    selector.setSelectorResourceEstimate(null);

    updated = resourceGroupManager.updateSelector(new SelectorsDetail(), selector, null);
    selectors = resourceGroupManager.readAllSelectors(null);

    Assert.assertEquals(selectors.size(), 2);
    Assert.assertEquals(updated, selectors.get(1));

    /* Create selector with an already existing resourceGroupId.
     *  In this case, new selector should be created. */
    selector.setResourceGroupId(3L);
    selector.setPriority(0L);
    selector.setUserRegex("new_user");
    selector.setSourceRegex("mode-scheduled");
    selector.setQueryType(null);
    selector.setClientTags(null);
    selector.setSelectorResourceEstimate(null);

    updated = resourceGroupManager.updateSelector(new SelectorsDetail(), selector, null);
    selectors = resourceGroupManager.readAllSelectors(null);

    Assert.assertEquals(selectors.size(), 3);
    Assert.assertEquals(updated, selectors.get(2));
  }

  @Test(dependsOnMethods = {"testUpdateSelector"})
  public void testDeleteSelector() {
    List<SelectorsDetail> selectors = resourceGroupManager.readAllSelectors(null);
    Assert.assertEquals(selectors.size(), 3);
    Assert.assertEquals(selectors.get(0).getResourceGroupId(), 0L);
    resourceGroupManager.deleteSelector(selectors.get(0), null);
    selectors = resourceGroupManager.readAllSelectors(null);

    Assert.assertEquals(selectors.size(), 2);
  }

  public void testCreateGlobalProperties() {
    GlobalPropertiesDetail globalPropertiesDetail = new GlobalPropertiesDetail();
    globalPropertiesDetail.setName("cpu_quota_period");
    globalPropertiesDetail.setValue("1h");

    GlobalPropertiesDetail newGlobalProperties =
        resourceGroupManager.createGlobalProperty(globalPropertiesDetail, null);

    Assert.assertEquals(newGlobalProperties, globalPropertiesDetail);

    try { // make sure that the name is cpu_quota_period
      GlobalPropertiesDetail invalidGlobalProperty = new GlobalPropertiesDetail();
      invalidGlobalProperty.setName("invalid_property");
      invalidGlobalProperty.setValue("1h");
      resourceGroupManager.createGlobalProperty(invalidGlobalProperty, null);
    } catch (Exception ex) {
      Assert.assertTrue(ex.getCause() instanceof org.h2.jdbc.JdbcSQLException);
      Assert.assertTrue(ex.getCause().getMessage().startsWith("Check constraint violation:"));
    }
  }

  @Test(dependsOnMethods = {"testCreateGlobalProperties"})
  public void testReadGlobalProperties() {
    List<GlobalPropertiesDetail> globalProperties = resourceGroupManager.readAllGlobalProperties(
            null);

    Assert.assertEquals(globalProperties.size(), 1);
    Assert.assertEquals(globalProperties.get(0).getName(), "cpu_quota_period");
    Assert.assertEquals(globalProperties.get(0).getValue(), "1h");
  }

  @Test(dependsOnMethods = {"testReadGlobalProperties"})
  public void testUpdateGlobalProperties() {
    GlobalPropertiesDetail globalPropertiesDetail = new GlobalPropertiesDetail();
    globalPropertiesDetail.setName("cpu_quota_period");
    globalPropertiesDetail.setValue("updated_test_value");

    GlobalPropertiesDetail updated =
        resourceGroupManager.updateGlobalProperty(globalPropertiesDetail, null);
    List<GlobalPropertiesDetail> globalProperties = resourceGroupManager.readAllGlobalProperties(
            null);

    Assert.assertEquals(globalProperties.size(), 1);
    Assert.assertEquals(updated, globalProperties.get(0));

    try { // make sure that the name is cpu_quota_period
      GlobalPropertiesDetail invalidGlobalProperty = new GlobalPropertiesDetail();
      invalidGlobalProperty.setName("invalid_property");
      invalidGlobalProperty.setValue("1h");
      resourceGroupManager.updateGlobalProperty(invalidGlobalProperty, null);
    } catch (Exception ex) {
      Assert.assertTrue(ex.getCause() instanceof org.h2.jdbc.JdbcSQLException);
      Assert.assertTrue(ex.getCause().getMessage().startsWith("Check constraint violation:"));
    }
  }

  public void testCreateExactMatchSourceSelectors() {
    ExactSelectorsDetail exactSelectorDetail = new ExactSelectorsDetail();

    exactSelectorDetail.setResourceGroupId("0");
    exactSelectorDetail.setUpdateTime("2020-07-06");
    exactSelectorDetail.setSource("@test@test_pipeline");
    exactSelectorDetail.setEnvironment("test");
    exactSelectorDetail.setQueryType("query_type");

    ExactSelectorsDetail newExactMatchSourceSelector =
        resourceGroupManager.createExactMatchSourceSelector(exactSelectorDetail);

    Assert.assertEquals(newExactMatchSourceSelector, exactSelectorDetail);
  }

  @Test(dependsOnMethods = {"testCreateExactMatchSourceSelectors"})
  public void testReadExactMatchSourceSelectors() {
    List<ExactSelectorsDetail> exactSelectorsDetails =
        resourceGroupManager.readExactMatchSourceSelector();

    Assert.assertEquals(exactSelectorsDetails.size(), 1);
    Assert.assertEquals(exactSelectorsDetails.get(0).getResourceGroupId(), "0");
    Assert.assertEquals(exactSelectorsDetails.get(0).getSource(), "@test@test_pipeline");
    Assert.assertEquals(exactSelectorsDetails.get(0).getEnvironment(), "test");
    Assert.assertEquals(exactSelectorsDetails.get(0).getQueryType(), "query_type");

    ExactSelectorsDetail exactSelector =
        resourceGroupManager.getExactMatchSourceSelector(exactSelectorsDetails.get(0));

    Assert.assertEquals(exactSelector.getResourceGroupId(), "0");
    Assert.assertEquals(exactSelector.getSource(), "@test@test_pipeline");
    Assert.assertEquals(exactSelector.getEnvironment(), "test");
    Assert.assertEquals(exactSelector.getQueryType(), "query_type");
  }

  @AfterClass(alwaysRun = true)
  public void cleanUp() {}
}
