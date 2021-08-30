package com.lyft.data.proxyserver;

import java.io.Closeable;
import java.io.File;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.util.TextUtils;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

@Slf4j
public class ProxyServer implements Closeable {
  private final Server server;
  private final ProxyServletImpl proxy;
  private final ProxyHandler proxyHandler;
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
      httpsConfig.setOutputBufferSize(32768);

      SecureRequestCustomizer src = new SecureRequestCustomizer();
      src.setStsMaxAge(TimeUnit.SECONDS.toSeconds(2000));
      src.setStsIncludeSubDomains(true);
      httpsConfig.addCustomizer(src);
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
    this.server.addConnector(connector);

    // Setup proxy handler to handle CONNECT methods
    ConnectHandler proxyConnectHandler = new ConnectHandler();
    this.server.setHandler(proxyConnectHandler);

    if (proxyHandler != null) {
      proxy.setProxyHandler(proxyHandler);
    }

    ServletHolder proxyServlet = new ServletHolder(config.getName(), proxy);

    proxyServlet.setInitParameter("proxyTo", config.getProxyTo());
    proxyServlet.setInitParameter("prefix", config.getPrefix());
    proxyServlet.setInitParameter("trustAll", config.getTrustAll());
    proxyServlet.setInitParameter("preserveHost", config.getPreserveHost());

    // Setup proxy servlet
    this.context =
        new ServletContextHandler(proxyConnectHandler, "/", ServletContextHandler.SESSIONS);
    this.context.addServlet(proxyServlet, "/*");
    this.context.addFilter(RequestFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
  }

  public void addFilter(Class<? extends Filter> filterClass, String pathSpec) {
    this.context.addFilter(filterClass, pathSpec, EnumSet.allOf(DispatcherType.class));
  }

  public void start() {

    try {
      this.server.start();
    } catch (Exception e) {
      log.error("Error starting proxy server", e);
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void close() {
    try {
      this.server.stop();
    } catch (Exception e) {
      log.error("Could not close the proxy server", e);
    }
  }
}
