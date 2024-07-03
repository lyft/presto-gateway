package com.lyft.data.proxyserver;

import java.io.Closeable;
import java.io.File;
import java.util.EnumSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.client.ResponseHandler;
import org.apache.http.util.TextUtils;
import org.eclipse.jetty.http.HttpParser;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

@Slf4j
public class ProxyServer implements Closeable {
  private final Server server;
  private final ProxyServletImpl proxy;
  private final ProxyHandler proxyHandler;
  private final ScheduledExecutorService scheduler =
      Executors.newScheduledThreadPool(1);

  private ServletContextHandler context;

  public ProxyServer(ProxyServerConfiguration config, ProxyHandler proxyHandler) {
    this(config, proxyHandler, new ProxyServletImpl());
  }

  public ProxyServer(ProxyServerConfiguration config, ProxyHandler proxyHandler,
                     ProxyServletImpl proxy) {
    this.server = new Server();
    this.server.setStopAtShutdown(true);
    this.proxy = proxy; 
    this.proxyHandler = proxyHandler;

    this.proxy.setServerConfig(config);
    this.setupContext(config);
  }

  private void setupContext(ProxyServerConfiguration config) {
    ServerConnector connector = null;

    if (config.isSsl()) {
      String keystorePath = config.getKeystorePath();
      String keystorePass = config.getKeystorePass();
      File keystoreFile = new File(keystorePath);

      SslContextFactory sslContextFactory = new SslContextFactory();
      sslContextFactory.setTrustAll(true);
      sslContextFactory.setStopTimeout(TimeUnit.SECONDS.toMillis(15));
      sslContextFactory.setSslSessionTimeout((int) TimeUnit.SECONDS.toMillis(15));


      if (!TextUtils.isBlank(keystorePath)) {
        sslContextFactory.setKeyStorePath(keystoreFile.getAbsolutePath());
        sslContextFactory.setKeyStorePassword(keystorePass);
        sslContextFactory.setKeyManagerPassword(keystorePass);
      }

      HttpConfiguration httpsConfig = new HttpConfiguration();
      httpsConfig.setSecureScheme(HttpScheme.HTTPS.asString());
      httpsConfig.setSecurePort(config.getLocalPort());
      httpsConfig.setIdleTimeout(150000);
      httpsConfig.setOutputBufferSize(32768);
      httpsConfig.setRequestHeaderSize(2048000);
      httpsConfig.setResponseHeaderSize(2048000);

      SecureRequestCustomizer src = new SecureRequestCustomizer();
      src.setStsMaxAge(TimeUnit.SECONDS.toSeconds(2000));
      src.setStsIncludeSubDomains(true);
      httpsConfig.addCustomizer(src);
      httpsConfig.addCustomizer(new org.eclipse.jetty.server.ForwardedRequestCustomizer());
      connector =
          new ServerConnector(
              server,
              new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
              new HttpConnectionFactory(httpsConfig));
    } else {
      connector = new ServerConnector(server);
    }
    connector.setHost("0.0.0.0");
    connector.setPort(config.getLocalPort());
    connector.setName(config.getName());
    connector.setAccepting(true);
    connector.setIdleTimeout(150000L);
    connector.setAcceptQueueSize(1024);
    this.server.addConnector(connector);

    // Setup proxy handler to handle CONNECT methods
    ConnectHandler proxyConnectHandler = new ConnectHandler();

    HandlerCollection handlers = new HandlerCollection();

    RequestLogHandler requestLogHandler = new RequestLogHandler();
    StatisticsHandler statsHandler = new StatisticsHandler();
    statsHandler.setHandler(proxyConnectHandler);

    //possible not needed
    //requestLogHandler.setRequestLog(customRequestLog);
    handlers.setHandlers(new Handler[] { requestLogHandler, statsHandler, proxyConnectHandler });

    this.server.setHandler(handlers);

    if (proxyHandler != null) {
      proxy.setProxyHandler(proxyHandler);

    }

    ServletHolder proxyServlet = new ServletHolder(config.getName(), proxy);
    proxyServlet.setInitParameter("proxyTo", config.getProxyTo());
    proxyServlet.setInitParameter("prefix", config.getPrefix());
    proxyServlet.setInitParameter("trustAll", config.getTrustAll());
    proxyServlet.setInitParameter("preserveHost", config.getPreserveHost());
    proxyServlet.setInitParameter("timeout", "120000");


    // Setup proxy servlet
    this.context =
        new ServletContextHandler(handlers, "/", ServletContextHandler.SESSIONS);
    this.context.addServlet(proxyServlet, "/*");
    this.context.addFilter(RequestFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
  }

  public void addFilter(Class<? extends Filter> filterClass, String pathSpec) {
    this.context.addFilter(filterClass, pathSpec, EnumSet.allOf(DispatcherType.class));
  }

  public void start() {

    try {
      this.server.start();

      // Schedule a task to log metrics at a fixed rate
      StatisticsHandler stats = this.server.getChildHandlerByClass(StatisticsHandler.class);
      this.scheduler.scheduleAtFixedRate(() -> {
        log.debug("(jetty) Num requests: " + stats.getRequests());
        log.debug("(jetty) Num active requests: " + stats.getRequestsActive());
        log.debug("(jetty) Responses with 4xx status: " + stats.getResponses4xx());
        log.debug("(jetty) Responses with 5xx status: " + stats.getResponses5xx());
        // Log other metrics as needed
      }, 0, 5, TimeUnit.MINUTES);
    } catch (Exception e) {
      log.error("Error starting proxy server", e);
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void close() {
    try {
      this.server.stop();
      this.scheduler.shutdown();
    } catch (Exception e) {
      log.error("Could not close the proxy server", e);
    }
  }
}
