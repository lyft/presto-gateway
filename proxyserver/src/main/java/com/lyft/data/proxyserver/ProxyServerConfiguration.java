package com.lyft.data.proxyserver;

import lombok.Data;

@Data
public class ProxyServerConfiguration {
  private String name;
  private int localPort;
  private String proxyTo;
  private String prefix = "/";
  private String trustAll = "true";
  private String preserveHost = "true";
}
