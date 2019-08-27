package com.lyft.data.gateway;

import com.lyft.data.baseapp.BaseApp;
import com.lyft.data.gateway.config.GatewayConfiguration;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.views.ViewBundle;
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

  public static void main(String[] args) throws Exception {
    // base package is scanned for any Resource class to be loaded by default.
    String basePackage = "com.lyft";
    new GatewayLauncher(basePackage).run(args);
  }
}
