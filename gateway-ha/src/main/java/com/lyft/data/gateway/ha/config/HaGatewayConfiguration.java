package com.lyft.data.gateway.ha.config;

import com.lyft.data.baseapp.AppConfiguration;
import com.lyft.data.gateway.config.NotifierConfiguration;
import com.lyft.data.gateway.config.RequestRouterConfiguration;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class HaGatewayConfiguration extends AppConfiguration {
  private RequestRouterConfiguration requestRouter;
  private NotifierConfiguration notifier;
  private DataStoreConfiguration dataStore;
}
