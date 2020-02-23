package com.lyft.data.proxyserver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.client.util.InputStreamContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import com.facebook.presto.sql.parser.ParsingException;
import com.google.common.io.CharStreams;
import com.lyft.data.proxyserver.wrapper.MultiReadHttpServletRequest;
import com.lyft.data.proxyserver.wrapper.TenantAwareQueryAdapter;
import com.lyft.data.proxyserver.wrapper.TenantId;
import com.lyft.data.proxyserver.wrapper.TenantLookupService;
import com.lyft.data.proxyserver.wrapper.TenantLookupServiceImpl;

@Slf4j
public class ProxyServletImpl extends ProxyServlet.Transparent {
  private ProxyHandler proxyHandler;
  private TenantAwareQueryAdapter tenantAwareQueryAdapter;
  public final static String MODIFIED_QUERY_HEADER  = "modifiedQuery";
  public final static String TENANT_ID_HEADER  = "tenantId";
  public final static String PRESTO_USER_HEADER = "X-Presto-User";
  public final static String INITIATED_HEADER = "initiatedts";

  public void setProxyHandler(ProxyHandler proxyHandler) {
    this.proxyHandler = proxyHandler;
    // This needs to be high as external clients may take longer to connect.
    this.setTimeout(TimeUnit.MINUTES.toMillis(1));
  }
  
  public void setTenantAwareQueryAdapter(TenantAwareQueryAdapter tenantAwareQueryAdapter) {
      this.tenantAwareQueryAdapter = tenantAwareQueryAdapter;
  }
  

// Overriding this method to support ssl
  @Override
  protected HttpClient newHttpClient() {
    SslContextFactory sslFactory = new SslContextFactory();
    sslFactory.setTrustAll(true);
    sslFactory.setStopTimeout(TimeUnit.SECONDS.toMillis(15));
    sslFactory.setSslSessionTimeout((int) TimeUnit.SECONDS.toMillis(15));

    HttpClient httpClient = new HttpClient(sslFactory);
    httpClient.setMaxConnectionsPerDestination(10000);
    httpClient.setConnectTimeout(TimeUnit.SECONDS.toMillis(60));
    return httpClient;
  }

  /** Customize the headers of forwarding proxy requests. */
  @Override
  protected void addProxyHeaders(HttpServletRequest request, Request proxyRequest) {
    super.addProxyHeaders(request, proxyRequest);
    if (proxyHandler != null) {
      proxyHandler.preConnectionHook(request, proxyRequest);
    }
  }
  
  /**
   * Rewrite our queries based on authentication header
   * 
   *  For urls:
   *  /v1 - presto proxy
   *  /entity - gateway config (adding/removing clusters)
   *  /gateway - gateway config (activating/deactivating clusters, nice if you want to a/b test some presto cluster config changes)
   *  
   *  We should only be exposing /v1 to the outside world and that's where auth should kick in. The other endpoints should
   *  be internal only. 
   */
    @Override
    protected ContentProvider proxyRequestContent(HttpServletRequest request, HttpServletResponse response, Request proxyRequest) throws IOException {
        if (tenantAwareQueryAdapter != null && request.getMethod().equals("POST") && request.getRequestURI().startsWith("/v1")) {
            TenantId tenantId = tenantAwareQueryAdapter.authenticate(proxyRequest.getHeaders().get(PRESTO_USER_HEADER));
            String requestBody = CharStreams.toString(request.getReader());

            String newBody = tenantAwareQueryAdapter.rewriteSql(requestBody, tenantId);
            log.info("Rewriting " + requestBody + " TO " + newBody.replace("\n", " ").toLowerCase().replace("\r", " ").replaceAll(" +", " ").trim());

            Integer contentLength = newBody.getBytes("UTF-8").length;
            // You have to null it out or you get duplicate Content-Length headers and the new one gets ignored
            proxyRequest.header(HttpHeader.CONTENT_LENGTH.asString(), null);
            proxyRequest.header(HttpHeader.CONTENT_LENGTH.asString(), contentLength.toString());
            proxyRequest.header(MODIFIED_QUERY_HEADER, newBody);
            proxyRequest.header(TENANT_ID_HEADER, tenantId.get());
            proxyRequest.header(INITIATED_HEADER, new Long(System.currentTimeMillis()).toString());
            return new InputStreamContentProvider(new ByteArrayInputStream(newBody.getBytes()));
        } 
        
        return new InputStreamContentProvider(request.getInputStream());
    }


  @Override
  protected String rewriteTarget(HttpServletRequest request) {
    String target = null;
    if (proxyHandler != null) {
      target = proxyHandler.rewriteTarget(request);
    }
    if (target == null) {
      target = super.rewriteTarget(request);
    }
    log.debug("Target : " + target);
    return target;
  }

  /**
   * Customize the response returned from remote server.
   *
   * @param request
   * @param response
   * @param proxyResponse
   * @param buffer
   * @param offset
   * @param length
   * @param callback
   */
  @Override
  protected void onResponseContent(
      HttpServletRequest request,
      HttpServletResponse response,
      Response proxyResponse,
      byte[] buffer,
      int offset,
      int length,
      Callback callback) {
    try {
      if (this._log.isDebugEnabled()) {
        this._log.debug(
            "[{}] proxying content to downstream: [{}] bytes", this.getRequestId(request), length);
      }
      if (this.proxyHandler != null) {
        proxyHandler.postConnectionHook(request, response, proxyResponse, buffer, offset, length, callback);
      } else {
        super.onResponseContent(request, response, proxyResponse, buffer, offset, length, callback);
      }
    } catch (Throwable var9) {
      callback.failed(var9);
    }
  }
  
  
}
