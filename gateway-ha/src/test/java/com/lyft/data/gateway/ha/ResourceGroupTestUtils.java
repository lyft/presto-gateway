package com.lyft.data.gateway.ha;

import com.lyft.data.gateway.ha.config.DataStoreConfiguration;
import com.lyft.data.gateway.ha.persistence.JdbcConnectionManager;

import java.io.InputStream;
import java.util.Random;
import java.util.Scanner;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.javalite.activejdbc.Base;


@Slf4j
public class ResourceGroupTestUtils {
  private static final OkHttpClient httpClient = new OkHttpClient();
  private static final Random RANDOM = new Random();

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TestConfig {
    private String configFilePath;
    private String h2DbFilePath;
  }

  public static void seedRequiredData(ResourceGroupTestUtils.TestConfig testConfig) {
    String jdbcUrl = "jdbc:h2:" + testConfig.getH2DbFilePath();
    DataStoreConfiguration db = new DataStoreConfiguration(jdbcUrl, "sa", "sa", "org.h2.Driver");
    JdbcConnectionManager connectionManager = new JdbcConnectionManager(db);
    connectionManager.open();
    Base.exec(getResourceFileContent("00-resource_groups.sql"));
    connectionManager.close();
  }

  public static String getResourceFileContent(String fileName) {
    StringBuilder sb = new StringBuilder();
    InputStream inputStream =
        ResourceGroupTestUtils.class.getClassLoader().getResourceAsStream(fileName);
    Scanner scn = new Scanner(inputStream);
    while (scn.hasNextLine()) {
      sb.append(scn.nextLine()).append("\n");
    }
    return sb.toString();
  }
}
