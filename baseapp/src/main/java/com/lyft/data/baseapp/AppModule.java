package com.lyft.data.baseapp;

import com.google.inject.AbstractModule;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.SimpleServerFactory;
import java.util.stream.Stream;
import lombok.Getter;

@Getter
public abstract class AppModule<T extends AppConfiguration, E> extends AbstractModule {
  private final T configuration;
  private final E environment;

  public AppModule(T config, E env) {
    this.configuration = config;
    this.environment = env;
  }

  @Override
  protected void configure() {}

  protected int getApplicationPort() {
    Stream<ConnectorFactory> connectors =
            configuration.getServerFactory() instanceof DefaultServerFactory
                    ? ((DefaultServerFactory) configuration.getServerFactory())
                    .getApplicationConnectors().stream()
                    : Stream.of((SimpleServerFactory) configuration.getServerFactory())
                    .map(SimpleServerFactory::getConnector);

    return connectors
            .filter(connector -> connector.getClass().isAssignableFrom(HttpConnectorFactory.class))
            .map(connector -> (HttpConnectorFactory) connector)
            .mapToInt(HttpConnectorFactory::getPort)
            .findFirst()
            .orElseThrow(IllegalStateException::new);
  }
}
