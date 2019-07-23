package com.lyft.data.gateway;

import com.lyft.data.gateway.config.ProxyBackendConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;

@Slf4j
public final class GatewayTestUtil {
  public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  private GatewayTestUtil() {}

  public static String buildGatewayConfigPath(
      int routerPort, List<ProxyBackendConfiguration> backends) throws IOException {
    StringBuilder sb = new StringBuilder();
    InputStream inputStream =
        GatewayTestUtil.class.getClassLoader().getResourceAsStream("test_config_template.yaml");
    Scanner scn = new Scanner(inputStream);
    while (scn.hasNextLine()) {
      sb.append(scn.nextLine()).append("\n");
    }
    File baseDir = new File(System.getProperty("java.io.tmpdir"));
    File tempDir = new File(baseDir, "temp-" + System.currentTimeMillis());
    tempDir.deleteOnExit();

    String configStr =
        sb.toString()
            .replace("REQUEST_ROUTER_PORT", String.valueOf(routerPort))
            .replace("CACHE_DIR", tempDir.getAbsolutePath())
            .replace(
                "APPLICATION_CONNECTOR_PORT", String.valueOf(30000 + (int) (Math.random() * 1000)))
            .replace("ADMIN_CONNECTOR_PORT", String.valueOf(31000 + (int) (Math.random() * 1000)));

    configStr += "\n" + "backends:\n";
    for (ProxyBackendConfiguration backend : backends) {
      configStr += "  - localPort: " + backend.getLocalPort() + "\n";
      configStr += "    name: " + backend.getName() + "\n";
      configStr += "    proxyTo: " + backend.getProxyTo() + "\n";
      configStr += "    routingGroup: " + backend.getRoutingGroup() + "\n";
    }

    File target = File.createTempFile("config-" + System.currentTimeMillis(), "config.yaml");

    FileWriter fw = new FileWriter(target);
    fw.append(configStr);
    fw.flush();
    log.info("Test Gateway Config \n[{}]", configStr);
    return target.getAbsolutePath();
  }

  public static ProxyBackendConfiguration getProxyBackendConfiguration(
      String name, String routingGroup, int backendPort, int localProxyPort) {
    ProxyBackendConfiguration proxyBackendConfiguration = new ProxyBackendConfiguration();
    proxyBackendConfiguration.setLocalPort(localProxyPort);
    proxyBackendConfiguration.setName(name);
    proxyBackendConfiguration.setRoutingGroup(routingGroup);
    proxyBackendConfiguration.setProxyTo("http://0.0.0.0:" + backendPort);
    proxyBackendConfiguration.setActive(true);
    proxyBackendConfiguration.setIncludeInRouter(true);
    return proxyBackendConfiguration;
  }
}
