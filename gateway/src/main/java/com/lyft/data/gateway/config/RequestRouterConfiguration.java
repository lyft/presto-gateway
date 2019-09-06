package com.lyft.data.gateway.config;

import lombok.Data;

@Data
public class RequestRouterConfiguration {
  // Local gateway port
  private int port;

  // Name of the routing gateway name (for metrics purposes)
  private String name;

  // Use SSL?
  private boolean ssl;
  private String keystorePath;
  private String keystorePass;

  private int historySize = 2000;
}
