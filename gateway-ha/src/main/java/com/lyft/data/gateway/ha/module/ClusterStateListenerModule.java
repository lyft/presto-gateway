package com.lyft.data.gateway.ha.module;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.lyft.data.baseapp.AppModule;
import com.lyft.data.gateway.ha.clustermonitor.HealthChecker;
import com.lyft.data.gateway.ha.clustermonitor.PrestoClusterStatsObserver;
import com.lyft.data.gateway.ha.config.HaGatewayConfiguration;
import com.lyft.data.gateway.ha.config.PagerDutyConfiguration;
import com.lyft.data.gateway.ha.notifier.PagerDutyNotifier;
import io.dropwizard.setup.Environment;
import java.util.ArrayList;
import java.util.List;

public class ClusterStateListenerModule extends AppModule<HaGatewayConfiguration, Environment> {
  List<PrestoClusterStatsObserver> observers;

  public ClusterStateListenerModule(HaGatewayConfiguration config, Environment env) {
    super(config, env);
  }

  /**
   * Observers to cluster stats updates from
   * {@link com.lyft.data.gateway.ha.clustermonitor.ActiveClusterMonitor}.
   *
   * @return
   */
  @Provides
  @Singleton
  public List<PrestoClusterStatsObserver> getClusterStatsObservers() {
    observers = new ArrayList<>();
    PagerDutyConfiguration pdConfig = getConfiguration().getPagerduty();
    observers.add(new HealthChecker(new PagerDutyNotifier(pdConfig)));
    return observers;
  }
}
