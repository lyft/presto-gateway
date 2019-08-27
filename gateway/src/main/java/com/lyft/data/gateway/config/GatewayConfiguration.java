package com.lyft.data.gateway.config;

import com.lyft.data.baseapp.AppConfiguration;
import java.util.List;
import lombok.Data;

@Data
public class GatewayConfiguration extends AppConfiguration {
  private RequestRouterConfiguration requestRouter;
  private NotifierConfiguration notifier;
  private List<ProxyBackendConfiguration> backends;
}
