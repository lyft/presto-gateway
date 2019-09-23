package com.lyft.data.gateway.ha.router;

import com.lyft.data.gateway.ha.HaGatewayTestUtils;
import com.lyft.data.gateway.ha.config.DataStoreConfiguration;
import com.lyft.data.gateway.ha.config.ProxyBackendConfiguration;
import com.lyft.data.gateway.ha.persistence.JdbcConnectionManager;
import java.io.File;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test
public class TestHaGatewayManager {
  private HaGatewayManager haGatewayManager;

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
    haGatewayManager = new HaGatewayManager(connectionManager);
  }

  public void testAddBackend() {
    ProxyBackendConfiguration backend = new ProxyBackendConfiguration();
    backend.setActive(true);
    backend.setRoutingGroup("adhoc");
    backend.setName("adhoc1");
    backend.setProxyTo("adhoc1.presto.lyft.com");
    ProxyBackendConfiguration updated = haGatewayManager.addBackend(backend);
    Assert.assertEquals(updated, backend);
  }

  @Test(dependsOnMethods = {"testAddBackend"})
  public void testGetBackends() {
    List<ProxyBackendConfiguration> backends = haGatewayManager.getAllBackends();
    Assert.assertEquals(backends.size(), 1);

    backends = haGatewayManager.getActiveBackends("adhoc");
    Assert.assertEquals(backends.size(), 1);

    backends = haGatewayManager.getActiveBackends("unknown");
    Assert.assertEquals(backends.size(), 0);

    backends = haGatewayManager.getActiveAdhocBackends();
    Assert.assertEquals(backends.size(), 1);
  }

  @Test(dependsOnMethods = {"testGetBackends"})
  public void testUpdateBackend() {
    ProxyBackendConfiguration backend = new ProxyBackendConfiguration();
    backend.setActive(false);
    backend.setRoutingGroup("adhoc");
    backend.setName("adhoc-lyft-1");
    backend.setProxyTo("adhoc1.presto.lyft.com");
    haGatewayManager.updateBackend(backend);
    List<ProxyBackendConfiguration> backends = haGatewayManager.getActiveBackends("adhoc");
    Assert.assertEquals(backends.size(), 1);

    backend.setActive(false);
    backend.setRoutingGroup("etl");
    backend.setName("adhoc1");
    backend.setProxyTo("adhoc1.presto.lyft.com");
    haGatewayManager.updateBackend(backend);
    backends = haGatewayManager.getActiveBackends("adhoc");
    Assert.assertEquals(backends.size(), 0);
    backends = haGatewayManager.getAllBackends();
    Assert.assertEquals(backends.size(), 2);
    Assert.assertEquals(backends.get(1).getRoutingGroup(), "etl");
  }

  @Test(dependsOnMethods = {"testUpdateBackend"})
  public void testDeleteBackend() {
    List<ProxyBackendConfiguration> backends = haGatewayManager.getAllBackends();
    Assert.assertEquals(backends.size(), 2);
    Assert.assertEquals(backends.get(1).getRoutingGroup(), "etl");
    haGatewayManager.deleteBackend(backends.get(0).getName());
    backends = haGatewayManager.getAllBackends();
    Assert.assertEquals(backends.size(), 1);
  }

  @AfterClass(alwaysRun = true)
  public void cleanUp() {}
}
