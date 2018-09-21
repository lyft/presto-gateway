package com.lyft.data.proxyserver;

import java.io.Closeable;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

@Slf4j
public class ProxyServer implements Closeable {
  private final Server server;
  private final ProxyHandler proxyHandler;
  private ServletContextHandler context;

  public ProxyServer(ProxyServerConfiguration config, ProxyHandler proxyHandler) {
    this.server = new Server();
    this.server.setStopAtShutdown(true);
    this.proxyHandler = proxyHandler;
    this.setupContext(config);
  }

  private void setupContext(ProxyServerConfiguration config) {
    ServerConnector connector = new ServerConnector(server);
    connector.setHost("0.0.0.0");
    connector.setPort(config.getLocalPort());
    connector.setName(config.getName());
    connector.setAccepting(true);
    this.server.addConnector(connector);

    // Setup proxy handler to handle CONNECT methods
    ConnectHandler proxyConnectHandler = new ConnectHandler();
    this.server.setHandler(proxyConnectHandler);

    ProxyImpl proxy = new ProxyImpl();
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
