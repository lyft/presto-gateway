package com.lyft.data.gateway.module;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.lyft.data.baseapp.AppModule;
import com.lyft.data.gateway.config.GatewayConfiguration;
import com.lyft.data.gateway.config.NotifierConfiguration;
import com.lyft.data.gateway.notifier.EmailNotifier;
import com.lyft.data.gateway.notifier.Notifier;
import io.dropwizard.setup.Environment;

public class NotifierModule extends AppModule<GatewayConfiguration, Environment> {

  public NotifierModule(GatewayConfiguration config, Environment env) {
    super(config, env);
  }

  @Provides
  @Singleton
  public Notifier provideNotifier() {
    NotifierConfiguration notifierConfiguration = getConfiguration().getNotifier();
    return new EmailNotifier(notifierConfiguration);
  }
}
