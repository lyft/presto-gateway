package com.lyft.data.gateway.ha.module;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.lyft.data.baseapp.AppModule;
import com.lyft.data.gateway.ha.clustermonitor.HealthChecker;
import com.lyft.data.gateway.ha.clustermonitor.PrestoClusterStatsObserver;
import com.lyft.data.gateway.ha.config.HaGatewayConfiguration;
import io.dropwizard.setup.Environment;

import java.util.ArrayList;
import java.util.List;

public class ClusterStateListenerModule extends AppModule<HaGatewayConfiguration, Environment> {

    public ClusterStateListenerModule(HaGatewayConfiguration config, Environment env) {
        super(config, env);
    }

    @Provides
    @Singleton
    public List<PrestoClusterStatsObserver> getClusterStatsObservers() {
        List<PrestoClusterStatsObserver> observers = new ArrayList<>();
        observers.add(new HealthChecker());
        return observers;
    }
}
