package com.lyft.data.gateway.ha.router;

import com.lyft.data.gateway.ha.HaGatewayTestUtils;
import com.lyft.data.gateway.ha.config.DataStoreConfiguration;
import com.lyft.data.gateway.ha.persistence.JdbcConnectionManager;
import com.lyft.data.gateway.router.QueryHistoryManager;
import com.lyft.data.gateway.router.QueryHistoryManager.QueryDetail;

import java.io.File;
import java.sql.Timestamp;
import java.util.List;
import org.javalite.activejdbc.Base;
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
    String jdbcUrl = "jdbc:h2:" + tempH2DbDir.getAbsolutePath();
    DataStoreConfiguration db = new DataStoreConfiguration(jdbcUrl, "sa", "sa", "org.h2.Driver");
    JdbcConnectionManager connectionManager = new JdbcConnectionManager(db);
    connectionManager.open();
    Base.exec(HaGatewayTestUtils.getResourceFileContent("gateway-ha-persistence.sql"));
    connectionManager.close();
    queryHistoryManager = new HaQueryHistoryManager(connectionManager) {};
  }

  public void testSubmitAndFetchQueryHistory() {
    List<QueryDetail> queryDetails = queryHistoryManager.fetchQueryHistory();
    Assert.assertEquals(queryDetails.size(), 0);
    QueryDetail queryDetail = new QueryDetail();
    queryDetail.setBackendUrl("http://localhost:9999");
    queryDetail.setSource("sqlWorkbench");
    queryDetail.setUser("test@ea.com");
    queryDetail.setQueryText("select 1");
    for (int i = 0; i < 2; i++) {
      queryDetail.setQueryId(String.valueOf(new Timestamp(System.currentTimeMillis()).getTime()));
      queryDetail.setCaptureTime(new Timestamp(System.currentTimeMillis()).getTime());
      queryHistoryManager.submitQueryDetail(queryDetail);
    }
    queryDetails = queryHistoryManager.fetchQueryHistory();
    Assert.assertEquals(queryDetails.size(), 2);
    Assert.assertTrue(queryDetails.get(0).getCaptureTime() > queryDetails.get(1).getCaptureTime());
  }
}
