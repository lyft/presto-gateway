package com.lyft.data.baseapp;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.AbstractModule;

import javax.annotation.Nonnull;

public class MetricRegistryModule extends AbstractModule {

  @Nonnull
  private final MetricRegistry metricsRegistry;

  public MetricRegistryModule(MetricRegistry metricsRegistry) {
    this.metricsRegistry = metricsRegistry;
  }

  @Override
  protected void configure() {
    bind(MetricRegistry.class).toInstance(metricsRegistry);
  }
}
