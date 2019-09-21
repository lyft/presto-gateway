package com.lyft.data.gateway.ha.router;

import com.lyft.data.gateway.ha.HaGatewayTestUtils;
import com.lyft.data.gateway.ha.config.DataStoreConfiguration;
import com.lyft.data.gateway.ha.persistence.JdbcConnectionManager;

import java.io.File;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test
public class TestQueryHistoryManager {
  private QueryHistoryManager queryHistoryManager;

  @BeforeClass(alwaysRun = true)
  public void setUp() {
    File baseDir = new File(System.getProperty("java.io.tmpdir"));
    File tempH2DbDir = new File(baseDir, "h2db-" + System.currentTimeMillis());
    tempH2DbDir.deleteOnExit();
    HaGatewayTestUtils.seedRequiredData(
        new HaGatewayTestUtils.TestConfig("", tempH2DbDir.getAbsolutePath()));
    String jdbcUrl = "jdbc:h2:" + tempH2DbDir.getAbsolutePath();
    DataStoreConfiguration db = new DataStoreConfiguration(jdbcUrl, "sa", "sa", "org.h2.Driver");
    JdbcConnectionManager connectionManager = new JdbcConnectionManager(db);
    queryHistoryManager = new HaQueryHistoryManager(connectionManager) {};
  }

  public void testSubmitAndFetchQueryHistory() {
    List<QueryHistoryManager.QueryDetail> queryDetails = queryHistoryManager.fetchQueryHistory();
    Assert.assertEquals(queryDetails.size(), 0);
    QueryHistoryManager.QueryDetail queryDetail = new QueryHistoryManager.QueryDetail();
    queryDetail.setBackendUrl("http://localhost:9999");
    queryDetail.setSource("sqlWorkbench");
    queryDetail.setUser("test@ea.com");
    queryDetail.setQueryText("select 1");
    for (int i = 0; i < 2; i++) {
      queryDetail.setQueryId(String.valueOf(System.currentTimeMillis()));
      queryDetail.setCaptureTime(System.currentTimeMillis());
      queryHistoryManager.submitQueryDetail(queryDetail);
    }
    queryDetails = queryHistoryManager.fetchQueryHistory();
    Assert.assertEquals(queryDetails.size(), 2);
    Assert.assertTrue(queryDetails.get(0).getCaptureTime() > queryDetails.get(1).getCaptureTime());
  }
}
