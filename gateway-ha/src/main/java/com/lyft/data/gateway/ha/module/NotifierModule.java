package com.lyft.data.gateway.ha.module;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.lyft.data.baseapp.AppModule;
import com.lyft.data.gateway.ha.config.HaGatewayConfiguration;
import com.lyft.data.gateway.ha.config.NotifierConfiguration;
import com.lyft.data.gateway.ha.notifier.EmailNotifier;
import com.lyft.data.gateway.ha.notifier.Notifier;
import io.dropwizard.setup.Environment;

public class NotifierModule extends AppModule<HaGatewayConfiguration, Environment> {

  public NotifierModule(HaGatewayConfiguration config, Environment env) {
    super(config, env);
  }

  @Provides
  @Singleton
  public Notifier provideNotifier() {
    NotifierConfiguration notifierConfiguration = getConfiguration().getNotifier();
    return new EmailNotifier(notifierConfiguration);
  }
}
