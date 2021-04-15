package com.lyft.data.gateway.ha.config;

import lombok.Data;

@Data
public class PagerDutyConfiguration {
  private String integrationKey;
  private String env;
  private String region;
}
