package com.lyft.data.gateway.ha.module;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.lyft.data.baseapp.AppModule;
import com.lyft.data.gateway.ha.clustermonitor.HealthChecker;
import com.lyft.data.gateway.ha.clustermonitor.PrestoClusterStatsObserver;
import com.lyft.data.gateway.ha.clustermonitor.PrestoQueueLengthChecker;
import com.lyft.data.gateway.ha.config.HaGatewayConfiguration;
import com.lyft.data.gateway.ha.config.NotifierConfiguration;
import com.lyft.data.gateway.ha.notifier.EmailNotifier;
import com.lyft.data.gateway.ha.router.PrestoQueueLengthRoutingTable;
import com.lyft.data.gateway.ha.router.RoutingManager;
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
  @Inject
  @Provides
  @Singleton
  public List<PrestoClusterStatsObserver> getClusterStatsObservers(RoutingManager routingManager) {
    observers = new ArrayList<>();
    NotifierConfiguration notifierConfiguration = getConfiguration().getNotifier();
    observers.add(new HealthChecker(new EmailNotifier(notifierConfiguration)));
    observers.add(new PrestoQueueLengthChecker((PrestoQueueLengthRoutingTable)routingManager));
    return observers;
  }
}
