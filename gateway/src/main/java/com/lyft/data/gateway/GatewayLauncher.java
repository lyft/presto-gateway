package com.lyft.data.gateway;

import com.lyft.data.gateway.app.AppModule;
import com.lyft.data.gateway.app.BaseApp;
import com.lyft.data.gateway.config.GatewayConfiguration;

import io.dropwizard.setup.Environment;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GatewayLauncher extends BaseApp<GatewayConfiguration> {

  public GatewayLauncher(String... packages) {
    super(packages);
  }

  @Override
  protected List<AppModule> addModules(
      GatewayConfiguration configuration, Environment environment) {
    List<AppModule> modules = new ArrayList<>();
    if (configuration.getModules() == null) {
      log.warn("No modules to load.");
      return modules;
    }
    for (String clazz : configuration.getModules()) {
      try {
        log.info("Trying to load module [{}]", clazz);
        Object ob =
            Class.forName(clazz)
                .getConstructor(GatewayConfiguration.class, Environment.class)
                .newInstance(configuration, environment);
        modules.add((AppModule) ob);
      } catch (Exception e) {
        log.error("Could not instantiate module [" + clazz + "]", e);
      }
    }
    return modules;
  }

  public static void main(String[] args) throws Exception {
    // base package is scanned for any Resource class to be loaded by default.
    String basePackage = "com.lyft";
    new GatewayLauncher(basePackage).run(args);
  }
}
