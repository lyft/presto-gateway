package com.lyft.data.gateway.ha;

import com.lyft.data.gateway.ha.config.DataStoreConfiguration;
import com.lyft.data.gateway.ha.persistence.JdbcConnectionManager;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.Base;

@Slf4j
public class HaGatewayTestUtils {
  @Data
  protected static class TestConfig {
    private String configFilePath;
    private String h2DbFilePath;
  }

  public static void seedRequiredData(TestConfig testConfig) {
    File baseDir = new File(System.getProperty("java.io.tmpdir"));
    File tempH2DbDir = new File(baseDir, "h2db-" + System.currentTimeMillis());
    tempH2DbDir.deleteOnExit();
    String jdbcUrl = "jdbc:h2:" + testConfig.getH2DbFilePath();
    DataStoreConfiguration db = new DataStoreConfiguration(jdbcUrl, "sa", "sa", "org.h2.Driver");
    JdbcConnectionManager connectionManager = new JdbcConnectionManager(db);
    connectionManager.open();
    Base.exec(HaGatewayTestUtils.getResourceFileContent("gateway-ha-persistence.sql"));
    connectionManager.close();
  }

  public static TestConfig buildGatewayConfigPath(int routerPort) throws IOException {
    TestConfig testConfig = new TestConfig();
    File baseDir = new File(System.getProperty("java.io.tmpdir"));
    File tempCacheDir = new File(baseDir, "temp-" + System.currentTimeMillis());
    tempCacheDir.deleteOnExit();

    File tempH2DbDir = new File(baseDir, "h2db-" + System.currentTimeMillis());
    tempH2DbDir.deleteOnExit();
    testConfig.setH2DbFilePath(tempH2DbDir.getAbsolutePath());

    String configStr =
        getResourceFileContent("test-config-template.yml")
            .replace("REQUEST_ROUTER_PORT", String.valueOf(routerPort))
            .replace("CACHE_DIR", tempCacheDir.getAbsolutePath())
            .replace("DB_FILE_PATH", tempH2DbDir.getAbsolutePath())
            .replace(
                "APPLICATION_CONNECTOR_PORT", String.valueOf(30000 + (int) (Math.random() * 1000)))
            .replace("ADMIN_CONNECTOR_PORT", String.valueOf(31000 + (int) (Math.random() * 1000)));

    File target = File.createTempFile("config-" + System.currentTimeMillis(), "config.yaml");

    FileWriter fw = new FileWriter(target);
    fw.append(configStr);
    fw.flush();
    log.info("Test Gateway Config \n[{}]", configStr);
    testConfig.setConfigFilePath(target.getAbsolutePath());
    seedRequiredData(testConfig);
    return testConfig;
  }

  public static String getResourceFileContent(String fileName) {
    StringBuilder sb = new StringBuilder();
    InputStream inputStream =
        HaGatewayTestUtils.class.getClassLoader().getResourceAsStream(fileName);
    Scanner scn = new Scanner(inputStream);
    while (scn.hasNextLine()) {
      sb.append(scn.nextLine()).append("\n");
    }
    return sb.toString();
  }
}
