package com.lyft.data.proxyserver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import com.google.common.io.CharStreams;
import com.lyft.data.proxyserver.wrapper.MultiReadHttpServletRequest;
import com.lyft.data.proxyserver.wrapper.TenantAwareQueryAdapter;

@Slf4j
public class ProxyServletImpl extends ProxyServlet.Transparent {
  private ProxyHandler proxyHandler;
  private TenantAwareQueryAdapter tenantAwareQueryAdapter;

  public void setProxyHandler(ProxyHandler proxyHandler) {
    this.proxyHandler = proxyHandler;
    // This needs to be high as external clients may take longer to connect.
    this.setTimeout(TimeUnit.MINUTES.toMillis(1));
  }
  
  public void setTenantAwareQueryAdapter(TenantAwareQueryAdapter tenantAwareQueryAdapter) {
      this.tenantAwareQueryAdapter = tenantAwareQueryAdapter;
  }
  
  @Override
  protected void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
      String requestBody = CharStreams.toString(request.getReader());
      System.out.println("Rewriting " + requestBody);
      
      /*
      if(requestBody.equalsIgnoreCase("select * from recipts limit 2")) {
          requestBody = "select * from recipts limit 1226";
          Integer contentLength = requestBody.getBytes("UTF-8").length;
          ((MultiReadHttpServletRequest) request).rewriteContent(requestBody.getBytes());
          ((MultiReadHttpServletRequest) request).addHeader(HttpHeader.CONTENT_LENGTH.asString(), contentLength.toString());
      }*/
      super.service(request, response);
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
      //tenantAwareQueryAdapter.
    }
    
  }
  
  // Happens after QueryIdCachingProxyHandler, result sets stored wrong
  
  @Override
  protected ContentProvider proxyRequestContent(HttpServletRequest request, HttpServletResponse response, Request proxyRequest) throws IOException {
      if (request.getMethod().equals("POST")
              && request.getRequestURI().startsWith("/v1/statement")) {
          String requestBody = CharStreams.toString(request.getReader());
          if(requestBody.equalsIgnoreCase("select * from recipts limit 2") || requestBody.equalsIgnoreCase("select * from recipts limit 1225")) {
              requestBody = "select * from recipts limit 1";
              Integer contentLength = requestBody.getBytes("UTF-8").length;
              
              ((MultiReadHttpServletRequest) request).rewriteContent(requestBody.getBytes());
              ((MultiReadHttpServletRequest) request).addHeader(HttpHeader.CONTENT_LENGTH.asString(), contentLength.toString());

              proxyRequest.header(HttpHeader.CONTENT_LENGTH.asString(), null);
              proxyRequest.header(HttpHeader.CONTENT_LENGTH.asString(), contentLength.toString());
              //proxyRequest.content(new BytesContentProvider(requestBody.getBytes("UTF-8")));
              
              return new InputStreamContentProvider(new ByteArrayInputStream(requestBody.getBytes()));
          } 
      }
      return new InputStreamContentProvider(request.getInputStream());
  }

  @Override
  protected String rewriteTarget(HttpServletRequest request) {
      //System.out.println(proxyRequest.getContent().toString());
      //long l = proxyRequest.getContent().toString().getBytes().length;
      //System.out.println(l);
      try {
        String requestBody = CharStreams.toString(request.getReader());
        System.out.println(requestBody);
    } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
      
      
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
        proxyHandler.postConnectionHook(request, response, buffer, offset, length, callback);
      } else {
        super.onResponseContent(request, response, proxyResponse, buffer, offset, length, callback);
      }
    } catch (Throwable var9) {
      callback.failed(var9);
    }
  }
  
  
}
