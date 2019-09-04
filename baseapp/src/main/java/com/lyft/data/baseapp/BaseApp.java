package com.lyft.data.baseapp;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import io.dropwizard.Application;
import io.dropwizard.Bundle;
import io.dropwizard.Configuration;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.servlets.tasks.Task;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Supports Guice in Dropwizard.
 *
 * <p>To use it, create a subclass and provide a list of modules you want to use with the {@link
 * #addModules} method.
 *
 * <p>Packages supplied in the constructor will be scanned for Resources, Tasks, Providers,
 * Healthchecks and Managed classes, and added to the environment. If you need to add anything to
 * the environment, or access the Injector at run time, you can use the {@link #applicationAtRun}
 * method.
 *
 * <p>GuiceApplication also makes {@link com.codahale.metrics.MetricRegistry} available for
 * injection.
 */
@Slf4j
public abstract class BaseApp<T extends AppConfiguration> extends Application<T> {

  private static final Logger logger = LoggerFactory.getLogger(BaseApp.class);

  private final Reflections reflections;
  private final List<Module> appModules = Lists.newArrayList();
  private Injector injector;

  protected BaseApp(String... basePackages) {
    final ConfigurationBuilder confBuilder = new ConfigurationBuilder();
    final FilterBuilder filterBuilder = new FilterBuilder();

    if (basePackages.length == 0) {
      basePackages = new String[] {};
    }

    logger.info("op=create auto_scan_packages={}", basePackages);

    for (String basePkg : basePackages) {
      confBuilder.addUrls(ClasspathHelper.forPackage(basePkg));
      filterBuilder.include(FilterBuilder.prefix(basePkg));
    }

    confBuilder
        .filterInputsBy(filterBuilder)
        .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner());

    this.reflections = new Reflections(confBuilder);
  }

  /**
   * Initializes the application bootstrap.
   *
   * @param bootstrap the application bootstrap
   */
  @Override
  public void initialize(Bootstrap<T> bootstrap) {
    super.initialize(bootstrap);
  }

  /**
   * When the application runs, this is called after the {@link Bundle}s are run.
   *
   * <p>You generally don't want to override this but if you do, make sure to call up into super to
   * allow the app to configure its Guice wiring correctly and apply anything you set up in {@link
   * #applicationAtRun}.
   *
   * @param configuration the parsed {@link Configuration} object
   * @param environment the application's {@link Environment}
   * @throws Exception if something goes wrong
   */
  @Override
  public void run(T configuration, Environment environment) throws Exception {
    this.injector = configureGuice(configuration, environment);
    logger.info("op=configure_guice injector={}", injector.toString());
    applicationAtRun(configuration, environment, injector);
    logger.info("op=configure_app_custom completed");
  }

  /**
   * Access the Dropwizard {@link Environment} and/or the Guice {@link Injector} when the
   * application is run. Override it to add providers, resources, etc. for your application as an
   * alternative to accessing {@link #run} .
   *
   * @param configuration the app configuration
   * @param environment the Dropwizard {@link Environment}
   * @param injector the Guice {@link Injector}
   */
  protected void applicationAtRun(T configuration, Environment environment, Injector injector) {}

  private Injector configureGuice(T configuration, Environment environment) throws Exception {
    appModules.add(new MetricRegistryModule(environment.metrics()));
    appModules.addAll(addModules(configuration, environment));
    Injector injector = Guice.createInjector(ImmutableList.copyOf(appModules));
    injector.injectMembers(this);
    registerWithInjector(configuration, environment, injector);
    return injector;
  }

  private void registerWithInjector(T configuration, Environment environment, Injector injector) {
    logger.info("op=register_start configuration={}", configuration.toString());
    registerHealthChecks(environment, injector);
    registerProviders(environment, injector);
    registerTasks(environment, injector);
    addManagedApps(configuration, environment, injector);
    registerResources(environment, injector);
    logger.info("op=register_end configuration={}", configuration.toString());
  }

  /**
   * Supply a list of modules to be used by Guice.
   *
   * @param configuration the app configuration
   * @return a list of modules to be provisioned by Guice
   */
  protected List<AppModule> addModules(T configuration, Environment environment) {
    List<AppModule> modules = new ArrayList<>();
    if (configuration.getModules() == null) {
      log.warn("No modules to load.");
      return modules;
    }
    for (String clazz : configuration.getModules()) {
      try {
        log.info("Trying to load module [{}]", clazz);
        Object ob =
            Class.forName(clazz)
                .getConstructor(configuration.getClass(), Environment.class)
                .newInstance(configuration, environment);
        modules.add((AppModule) ob);
      } catch (Exception e) {
        log.error("Could not instantiate module [" + clazz + "]", e);
      }
    }
    return modules;
  }

  /**
   * Supply a list of managed apps.
   *
   * @param configuration
   * @param environment
   * @param injector
   * @return
   */
  protected List<Managed> addManagedApps(
      T configuration, Environment environment, Injector injector) {
    List<Managed> managedApps = new ArrayList<>();
    if (configuration.getManagedApps() == null) {
      log.error("No managed apps found");
      return managedApps;
    }
    configuration
        .getManagedApps()
        .forEach(
            clazz -> {
              try {
                Class c = Class.forName(clazz);
                LifecycleEnvironment lifecycle = environment.lifecycle();
                lifecycle.manage((Managed) injector.getInstance(c));
                log.info("op=register type=managed item={}", c);
              } catch (Exception e) {
                log.error("Error loading managed app", e);
              }
            });
    return managedApps;
  }

  private void registerTasks(Environment environment, Injector injector) {
    final Set<Class<? extends Task>> classes = reflections.getSubTypesOf(Task.class);
    classes.forEach(
        c -> {
          environment.admin().addTask(injector.getInstance(c));
          logger.info("op=register type=task item={}", c);
        });
  }

  private void registerHealthChecks(Environment environment, Injector injector) {
    final Set<Class<? extends HealthCheck>> classes = reflections.getSubTypesOf(HealthCheck.class);
    classes.forEach(
        c -> {
          environment.healthChecks().register(c.getSimpleName(), injector.getInstance(c));
          logger.info("op=register type=healthcheck item={}", c);
        });
  }

  private void registerProviders(Environment environment, Injector injector) {
    final Set<Class<?>> classes = reflections.getTypesAnnotatedWith(Provider.class);
    classes.forEach(
        c -> {
          environment.jersey().register(injector.getInstance(c));
          logger.info("op=register type=provider item={}", c);
        });
  }

  private void registerResources(Environment environment, Injector injector) {
    final Set<Class<?>> classes = reflections.getTypesAnnotatedWith(Path.class);
    classes.forEach(
        c -> {
          environment.jersey().register(injector.getInstance(c));
          logger.info("op=register type=resource item={}", c);
        });
  }
}
