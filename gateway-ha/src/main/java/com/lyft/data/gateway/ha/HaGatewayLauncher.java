package com.lyft.data.gateway.ha;

import com.lyft.data.baseapp.BaseApp;
import com.lyft.data.gateway.ha.config.HaGatewayConfiguration;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.views.ViewBundle;

public class HaGatewayLauncher extends BaseApp<HaGatewayConfiguration> {

  public HaGatewayLauncher(String... basePackages) {
    super(basePackages);
  }

  @Override
  public void initialize(Bootstrap<HaGatewayConfiguration> bootstrap) {
    super.initialize(bootstrap);
    bootstrap.addBundle(new ViewBundle<>());
    bootstrap.addBundle(new AssetsBundle("/assets", "/assets", null, "assets"));
  }

  public static void main(String[] args) throws Exception {
    /** base package is scanned for any Resource class to be loaded by default. */
    String basePackage = "com.lyft";
    new HaGatewayLauncher(basePackage).run(args);
  }
}
