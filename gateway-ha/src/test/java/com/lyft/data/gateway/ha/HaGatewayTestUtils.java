package com.lyft.data.gateway.ha;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HaGatewayTestUtils {

  public static String buildGatewayConfigPath(int routerPort) throws IOException {

    File baseDir = new File(System.getProperty("java.io.tmpdir"));
    File tempCacheDir = new File(baseDir, "temp-" + System.currentTimeMillis());
    tempCacheDir.deleteOnExit();

    File tempH2DbDir = new File(baseDir, "h2db-" + System.currentTimeMillis());
    tempH2DbDir.deleteOnExit();

    String configStr =
        getResourceFileContent("test_config_template.yaml")
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
    return target.getAbsolutePath();
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
