package com.lyft.data.gateway.ha.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataStoreConfiguration {
  private String jdbcUrl;
  private String user;
  private String password;
  private String driver;
}
