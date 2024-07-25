package com.lyft.data.proxyserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.Closeable;
import java.io.File;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.Slf4jRequestLogWriter;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

@Slf4j
public class ProxyServer implements Closeable {
  private final Server server;
  private final ProxyServletImpl proxy;
  private final ProxyHandler proxyHandler;
  private ServletContextHandler context;
  private boolean accessLogEnabled;

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
    //cheap flag to turn on Jetty access logging
    this.accessLogEnabled = false;
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
      httpsConfig.setRequestHeaderSize(2048000);
      httpsConfig.setResponseHeaderSize(2048000);

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
    connector.setIdleTimeout(150000L);
    connector.setAcceptQueueSize(1024);
    this.server.addConnector(connector);

    // Setup proxy handler to handle CONNECT methods
    ConnectHandler proxyConnectHandler = new ConnectHandler();

    HandlerCollection handlers = new HandlerCollection();

    if (this.accessLogEnabled) {
      Slf4jRequestLogWriter slfjRequestLogWriter = new Slf4jRequestLogWriter();
      slfjRequestLogWriter.setLoggerName("request.log");
      String myFormat =
          "ACCESS LOG %{client}a - %u %t \"%r\" %s %O \"%{Referer}i\" \"%{User-Agent}i\" **%T/%D**";
  
  
      CustomRequestLog requestLog = new CustomRequestLog(slfjRequestLogWriter,myFormat) {
        @Override
        public void log(Request request, Response response) {
          String clientAddress = request.getRemoteAddr();
          String username = request.getRemoteUser();
          String requestTime = String.valueOf(request.getTimeStamp());
          String requestMethod = request.getMethod();
          String requestUri = request.getRequestURI();
          int responseStatus = response.getStatus();
          long responseSize = response.getContentCount();
          String referer = request.getHeader("Referer");
          String userAgent = request.getHeader("User-Agent");
          long requestDurationMs = System.currentTimeMillis() - request.getTimeStamp();
  
          String logMessageString = "ACCESS LOG == " + clientAddress + " - "
                  + (username != null ? username : "-") + " " + requestTime
                  + " \"" + requestMethod + " " + requestUri + " " + request.getProtocol()
                  + "\" " + responseStatus + " " + responseSize + " \""
                  + (referer != null ? referer : "-") + "\" \""
                  + (userAgent != null ? userAgent : "-")
                  + "\" **" + (requestDurationMs / 1000) + "/" + requestDurationMs + "**";
  
          ObjectMapper mapper = new ObjectMapper();
          ObjectNode logMessage = mapper.createObjectNode();
          logMessage.put("clientAddress", clientAddress);
          logMessage.put("username", username != null ? username : "-");
          logMessage.put("requestTime", requestTime);
          logMessage.put("requestMethod", requestMethod);
          logMessage.put("requestURI", requestUri);
          logMessage.put("protocol", request.getProtocol());
          logMessage.put("responseStatus", responseStatus);
          logMessage.put("responseSize", responseSize);
          logMessage.put("referer", referer != null ? referer : "-");
          logMessage.put("userAgent", userAgent != null ? userAgent : "-");
          logMessage.put("requestDurationMs", requestDurationMs);
          try {
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
              String header = headerNames.nextElement();
              logMessage.put("request_header_" + header, request.getHeader(header));
            }
  
            for (String i: response.getHeaderNames()) {
              logMessage.put("request_header_" + i, response.getHeader(i));
            }
            
            log.info("ACCESS LOG: {} : {}", logMessage.toString(),
                      request.getHeader("proxytarget"), request.getHeaderNames());
            
          } catch (Exception e) {
            log.error("Error logging access log message", e);
          }
  
  
          //log.info(logMessageString);
        }
      };
      
      this.server.setRequestLog(customRequestLog);
    }
    
    handlers.setHandlers(new Handler[] { proxyConnectHandler });
    this.server.setHandler(handlers);

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
