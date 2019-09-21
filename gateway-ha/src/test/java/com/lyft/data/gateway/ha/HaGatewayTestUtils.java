package com.lyft.data.gateway.ha;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.lyft.data.gateway.ha.config.DataStoreConfiguration;
import com.lyft.data.gateway.ha.persistence.JdbcConnectionManager;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.Scanner;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.javalite.activejdbc.Base;
import org.testng.Assert;

@Slf4j
public class HaGatewayTestUtils {
  private static final OkHttpClient httpClient = new OkHttpClient();
  private static final Random RANDOM = new Random();

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TestConfig {
    private String configFilePath;
    private String h2DbFilePath;
  }

  public static void seedRequiredData(TestConfig testConfig) {
    String jdbcUrl = "jdbc:h2:" + testConfig.getH2DbFilePath();
    DataStoreConfiguration db = new DataStoreConfiguration(jdbcUrl, "sa", "sa", "org.h2.Driver");
    JdbcConnectionManager connectionManager = new JdbcConnectionManager(db);
    connectionManager.open();
    Base.exec(HaGatewayTestUtils.getResourceFileContent("gateway-ha-persistence.sql"));
    connectionManager.close();
  }

  public static void prepareMockBackend(
      WireMockServer backend, String endPoint, String expectedResonse) {
    backend.start();
    backend.stubFor(
        WireMock.post(endPoint)
            .willReturn(
                WireMock.aResponse()
                    .withBody(expectedResonse)
                    .withHeader("Content-Encoding", "plain")
                    .withStatus(200)));
  }

  public static TestConfig buildGatewayConfigAndSeedDb(int routerPort) throws IOException {
    TestConfig testConfig = new TestConfig();
    File baseDir = new File(System.getProperty("java.io.tmpdir"));
    File tempH2DbDir = new File(baseDir, "h2db-" + RANDOM.nextInt() + System.currentTimeMillis());
    tempH2DbDir.deleteOnExit();
    testConfig.setH2DbFilePath(tempH2DbDir.getAbsolutePath());

    String configStr =
        getResourceFileContent("test-config-template.yml")
            .replace("REQUEST_ROUTER_PORT", String.valueOf(routerPort))
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

  public static void setUpBackend(
      String name, String proxyTo, boolean active, String routingGroup, int routerPort)
      throws Exception {
    RequestBody requestBody =
        RequestBody.create(
            MediaType.parse("application/json; charset=utf-8"),
            "{ \"name\": \""
                + name
                + "\",\"proxyTo\": \""
                + proxyTo
                + "\",\"active\": "
                + active
                + ",\"routingGroup\": \""
                + routingGroup
                + "\"}");
    Request request =
        new Request.Builder()
            .url("http://localhost:" + routerPort + "/entity?entityType=GATEWAY_BACKEND")
            .post(requestBody)
            .build();
    Response response = httpClient.newCall(request).execute();
    Assert.assertTrue(response.isSuccessful());
  }
}
