package com.lyft.data.gateway.handler;

import com.codahale.metrics.Meter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import com.lyft.data.gateway.config.GatewayConfiguration;
import com.lyft.data.gateway.router.GatewayBackendManager;
import com.lyft.data.gateway.router.QueryHistoryManager;
import com.lyft.data.gateway.router.RoutingManager;
import com.lyft.data.gateway.router.impl.DefaultRoutingManager;
import com.lyft.data.proxyserver.ProxyHandler;
import com.lyft.data.proxyserver.wrapper.MultiReadHttpServletRequest;

import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.SimpleServerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.Callback;

@Slf4j
public class QueryIdCachingProxyHandler extends ProxyHandler {
  public static final String PROXY_TARGET_HEADER = "proxytarget";
  public static final String V1_STATEMENT_PATH = "/v1/statement";
  public static final String V1_QUERY_PATH = "/v1/query";
  public static final String QUERY_HTML_PATH = "/ui/query.html";
  public static final String SCHEDULED_QUERY_HEADER = "X-Presto-Scheduled-Query";
  public static final String USER_HEADER = "X-Presto-User";
  public static final String SOURCE_HEADER = "X-Presto-Source";
  private static final Pattern EXTRACT_BETWEEN_SINGLE_QUOTES = Pattern.compile("'([^\\s']+)'");

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final RoutingManager routingManager;
  private final QueryHistoryManager queryHistoryManager;

  private final Meter requestMeter;
  private final int localApplicationPort;

  public QueryIdCachingProxyHandler(
      GatewayBackendManager gatewayBackendManager,
      QueryHistoryManager queryHistoryManager,
      GatewayConfiguration appConfig,
      Meter requestMeter) {
    this.requestMeter = requestMeter;
    this.routingManager =
        new DefaultRoutingManager(
            gatewayBackendManager, appConfig.getRequestRouter().getCacheDir());
    this.queryHistoryManager = queryHistoryManager;
    this.localApplicationPort = getApplicationPort(appConfig);
  }

  protected RoutingManager getRoutingManager() {
    return this.routingManager;
  }

  private int getApplicationPort(GatewayConfiguration configuration) {
    Stream<ConnectorFactory> connectors =
        configuration.getServerFactory() instanceof DefaultServerFactory
            ? ((DefaultServerFactory) configuration.getServerFactory())
                .getApplicationConnectors()
                .stream()
            : Stream.of((SimpleServerFactory) configuration.getServerFactory())
                .map(SimpleServerFactory::getConnector);

    int port =
        connectors
            .filter(connector -> connector.getClass().isAssignableFrom(HttpConnectorFactory.class))
            .map(connector -> (HttpConnectorFactory) connector)
            .mapToInt(HttpConnectorFactory::getPort)
            .findFirst()
            .orElseThrow(IllegalStateException::new);
    return port;
  }

  @Override
  public void preConnectionHook(HttpServletRequest request, Request proxyRequest) {
    if (request.getMethod().equals(HttpMethod.POST)
        && request.getRequestURI().startsWith(V1_STATEMENT_PATH)) {
      requestMeter.mark();
      try {
        String requestBody = CharStreams.toString(request.getReader());
        log.info(
            "Processing request endpoint: [{}], payload: [{}]",
            request.getRequestURI(),
            requestBody);
      } catch (Exception e) {
        log.warn("Error fetching the request payload", e);
      }
    }
  }

  @Override
  public String rewriteTarget(HttpServletRequest request) {
    /* Here comes the load balancer / gateway */
    String backendAddress = "http://localhost:" + localApplicationPort;

    // Only load balance presto query APIs.
    if (request.getRequestURI().startsWith(V1_STATEMENT_PATH)
        || request.getRequestURI().startsWith(V1_QUERY_PATH)) {
      String queryId = extractQueryIdIfPresent(request);

      // Find query id and get url from cache
      if (!Strings.isNullOrEmpty(queryId)) {
        backendAddress = routingManager.findBackendForQueryId(queryId);
      } else {
        String scheduledHeader = request.getHeader(SCHEDULED_QUERY_HEADER);
        if ("true".equalsIgnoreCase(scheduledHeader)) {
          // This falls back on adhoc backends if there are no scheduled backends active.
          backendAddress = routingManager.provideScheduledBackendForThisRequest();
        } else {
          backendAddress = routingManager.provideAdhocBackendForThisRequest();
        }
      }
      // set target backend so that we could save queryId to backend mapping later.
      ((MultiReadHttpServletRequest) request).addHeader(PROXY_TARGET_HEADER, backendAddress);
    }
    String targetLocation =
        backendAddress
            + request.getRequestURI()
            + (request.getQueryString() != null ? "?" + request.getQueryString() : "");

    String originalLocation =
        request.getRemoteAddr()
            + request.getRequestURI()
            + (request.getQueryString() != null ? "?" + request.getQueryString() : "");

    log.info("Rerouting [{}]--> [{}]", originalLocation, targetLocation);
    return targetLocation;
  }

  protected String extractQueryIdIfPresent(HttpServletRequest request) {
    String path = request.getRequestURI();
    String queryParams = request.getQueryString();
    try {
      String queryText = CharStreams.toString(request.getReader());
      if (!Strings.isNullOrEmpty(queryText)
          && queryText.toLowerCase().contains("system.runtime.kill_query")) {
        // extract and return the queryId
        String[] parts = queryText.split(",");
        for (String part : parts) {
          if (part.contains("query_id")) {
            Matcher m = EXTRACT_BETWEEN_SINGLE_QUOTES.matcher(part);
            if (m.find()) {
              String queryQuoted = m.group();
              if (!Strings.isNullOrEmpty(queryQuoted) && queryQuoted.length() > 0) {
                return queryQuoted.substring(1, queryQuoted.length() - 1);
              }
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("Error extracting query payload from request", e);
    }
    if (path == null) {
      return null;
    }
    String queryId = null;

    log.debug("trying to extract query id from path [{}] or queryString [{}]", path, queryParams);
    if (path.startsWith(V1_STATEMENT_PATH) || path.startsWith(V1_QUERY_PATH)) {
      String[] tokens = path.split("/");
      if (tokens.length >= 4) {
        queryId = tokens[3];
      }
    } else if (path.startsWith(QUERY_HTML_PATH)) {
      queryId = queryParams;
    }
    log.debug("query id in url [{}]", queryId);
    return queryId;
  }

  /**
   * Response interceptor default.
   *
   * @param request
   * @param response
   * @param buffer
   * @param offset
   * @param length
   * @param callback
   */
  protected void postConnectionHook(
      HttpServletRequest request,
      HttpServletResponse response,
      byte[] buffer,
      int offset,
      int length,
      Callback callback) {
    try {
      String requestPath = request.getRequestURI();
      if (requestPath.startsWith(V1_STATEMENT_PATH)
          && request.getMethod().equals(HttpMethod.POST)) {
        String output;
        boolean isGZipEncoding = isGZipEncoding(response);
        if (isGZipEncoding) {
          output = plainTextFromGz(buffer);
        } else {
          output = new String(buffer);
        }
        log.debug("Response output [{}]", output);

        QueryHistoryManager.QueryDetail queryDetail = getQueryDetailsFromRequest(request);
        log.debug("Proxy destination : {}", queryDetail.getBackendUrl());

        if (response.getStatus() == HttpStatus.OK_200) {
          HashMap<String, String> results = OBJECT_MAPPER.readValue(output, HashMap.class);
          queryDetail.setQueryId(results.get("id"));

          if (!Strings.isNullOrEmpty(queryDetail.getQueryId())) {
            routingManager.setBackendForQueryId(
                queryDetail.getQueryId(), queryDetail.getBackendUrl());
            log.debug(
                "QueryId [{}] mapped with proxy [{}]",
                queryDetail.getQueryId(),
                queryDetail.getBackendUrl());
          } else {
            log.debug("QueryId [{}] could not be cached", queryDetail.getQueryId());
          }
        } else {
          log.error(
              "Non OK HTTP Status code with response [{}] , Status code [{}]",
              output,
              response.getStatus());
        }
        // Saving history at gateway.
        queryHistoryManager.submitQueryDetail(queryDetail);
      } else {
        log.debug("SKIPPING For {}", requestPath);
      }
    } catch (Exception e) {
      log.error("Error in proxying defaulting to super call", e);
    }
    super.postConnectionHook(request, response, buffer, offset, length, callback);
  }

  private QueryHistoryManager.QueryDetail getQueryDetailsFromRequest(HttpServletRequest request)
      throws IOException {
    QueryHistoryManager.QueryDetail queryDetail = new QueryHistoryManager.QueryDetail();
    queryDetail.setBackendUrl(request.getHeader(PROXY_TARGET_HEADER));
    queryDetail.setCaptureTime(System.currentTimeMillis());
    queryDetail.setUser(request.getHeader(USER_HEADER));
    queryDetail.setSource(request.getHeader(SOURCE_HEADER));
    String queryText = CharStreams.toString(request.getReader());
    queryDetail.setQueryText(
        queryText.length() > 400 ? queryText.substring(0, 400) + "..." : queryText);
    return queryDetail;
  }
}
