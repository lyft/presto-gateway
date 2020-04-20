package com.lyft.data.gateway.ha.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DummyHealthCheck extends HealthCheck {
  @Override
  protected Result check() throws Exception {
    log.debug("running healthcheck");
    return Result.healthy();
  }
}