package com.lyft.data.gateway;

import com.google.inject.Injector;
import com.lyft.data.baseapp.AppModule;
import com.lyft.data.baseapp.BaseApp;
import com.lyft.data.gateway.config.GatewayConfiguration;

import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GatewayLauncher extends BaseApp<GatewayConfiguration> {

  public GatewayLauncher(String... packages) {
    super(packages);
  }

  @Override
  public void initialize(Bootstrap<GatewayConfiguration> bootstrap) {
    super.initialize(bootstrap);
    bootstrap.addBundle(new ViewBundle<>());
    bootstrap.addBundle(new AssetsBundle("/assets", "/assets"));
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

  protected List<Managed> addManagedApps(
      GatewayConfiguration configuration, Environment environment, Injector injector) {
    List<Managed> managedApps = new ArrayList<>();
    if (configuration.getManagedApps() == null) {
      log.error("No managed apps found");
      return managedApps;
    }
    configuration
        .getManagedApps()
        .stream()
        .forEach(
            clazz -> {
              try {
                Class c = Class.forName(clazz);
                LifecycleEnvironment lifecycle = environment.lifecycle();
                lifecycle.manage((Managed) injector.getInstance(c));
                log.info("op=register type=managed item={}", c);
              } catch (Exception e) {
                log.error("Error loading managed app", e);
              }
            });
    return managedApps;
  }

  public static void main(String[] args) throws Exception {
    // base package is scanned for any Resource class to be loaded by default.
    String basePackage = "com.lyft";
    new GatewayLauncher(basePackage).run(args);
  }
}
