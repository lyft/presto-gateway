package com.lyft.data.gateway.ha.config;

import java.util.List;
import lombok.Data;

@Data
public class NotifierConfiguration {
  private boolean startTlsEnabled;
  private boolean smtpAuthEnabled;
  private String smtpHost = "localhost";
  private int smtpPort = 587;
  private String smtpUser;
  private String smtpPassword;
  private String sender;
  private List<String> recipients;
}
