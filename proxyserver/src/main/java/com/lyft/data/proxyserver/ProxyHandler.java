package com.lyft.data.proxyserver;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.util.Callback;

/* Order of control => rewriteTarget, preConnectionHook, postConnectionHook. */
@Slf4j
public class ProxyHandler {

  protected String rewriteTarget(HttpServletRequest request) {
    // Dont override this unless absolutely needed.
    return null;
  }

  /**
   * Request interceptor.
   *
   * @param request
   * @param proxyRequest
   */
  public void preConnectionHook(HttpServletRequest request, Request proxyRequest) {
    // you may override it.
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
      if (response.getStatus() == HttpServletResponse.SC_BAD_GATEWAY) {
        log.error("Received 502 Bad Gateway response for request URI: {}",
                request.getRequestURL());
      }
      response.getOutputStream().write(buffer, offset, length);
      // [sev-16337] with a 1% probably, log the request and response headers
      // and size for debugging
      if (Math.random() < 0.01) {
        log.debug("Request URL: {} , request URI {} , servlet path {} ,"
          + "toString {}, getContentLength {}, getRequestHeaderSize {}, getResponseHeaderSize {}",
                request.getRequestURL(), request.getRequestURI(), request.getServletPath(),
                request.toString(), request.getContentLength(), getRequestHeaderSize(request), getResponseHeaderSize(response));
      }

      callback.succeeded();
    } catch (Throwable var9) {
      log.error("Exception occurred while processing request URL: {} , request URI {} ,"
                + " servlet path {} , toString {}, getContentLength {}, getRequestHeaderSize {},"
                + "getResponseHeaderSize {}, requestHeaders {}, responseHeaders {}", 
                request.getRequestURL(), request.getRequestURI(), request.getServletPath(), request.toString(),
              request.getContentLength(), getRequestHeaderSize(request), getRequestHeaderSize(response), 
                errorLogHeaders(request), errorLogHeaders(response), var9);
      
      callback.failed(var9);
    }
  }

  protected int getRequestHeaderSize(HttpServletRequest request) {
    int headerSize = 0;
    Enumeration<String> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
        String headerName = headerNames.nextElement();
        String headerValue = request.getHeader(headerName);
        headerSize += headerName.length() + headerValue.length();
    }
    return headerSize;
}

private int getResponseHeaderSize(HttpServletResponse response) {
  int headerSize = 0;
  for (String headerName : response.getHeaderNames()) {
      String headerValue = response.getHeader(headerName);
      headerSize += headerName.length() + headerValue.length();
  }
  return headerSize;
}

  protected String errorLogHeaders(HttpServletRequest request) {
    StringBuilder sb = "------- error HttpServletRequest headers---------";
    Enumeration<String> headers = request.getHeaderNames();
    while (headers.hasMoreElements()) {
      String header = headers.nextElement();
      sb.append(header);
    }
    return sb.toString();
  }

  protected String errorLogHeaders(HttpServletResponse response) {
    StringBuilder sb = "------- error HttpServletResponse headers---------";
    Enumeration<String> headers = request.getHeaderNames();
    while (headers.hasMoreElements()) {
      String header = headers.nextElement();
      sb.append(header);
    }
    return sb.toString();
  }

  protected void debugLogHeaders(HttpServletRequest request) {
    if (log.isDebugEnabled()) {
      log.debug("-------HttpServletRequest headers---------");
      Enumeration<String> headers = request.getHeaderNames();
      while (headers.hasMoreElements()) {
        String header = headers.nextElement();
        log.debug(header + "->" + request.getHeader(header));
      }
    }
  }

  protected void debugLogHeaders(HttpServletResponse response) {
    if (log.isDebugEnabled()) {
      log.debug("-------HttpServletResponse headers---------");
      Collection<String> headers = response.getHeaderNames();
      for (String header : headers) {
        log.debug(header + "->" + response.getHeader(header));
      }
    }
  }

  protected void debugLogHeaders(Request proxyRequest) {
    if (log.isDebugEnabled()) {
      log.debug("-------Request proxyRequest headers---------");
      HttpFields httpFields = proxyRequest.getHeaders();
      log.debug(httpFields.toString());
    }
  }

  protected void setProxyHeader(Request proxyRequest, String key, String value) {
    if (key == null || value == null) {
      return;
    }
    log.debug("Setting header [{}] with value [{}]", key, value);
    proxyRequest.getHeaders().remove(key);
    proxyRequest.header(key, value);
  }

  protected boolean isGZipEncoding(HttpServletResponse response) {
    String contentEncoding = response.getHeader(HttpHeaders.CONTENT_ENCODING);
    return contentEncoding != null && contentEncoding.toLowerCase().contains("gzip");
  }

  protected String plainTextFromGz(byte[] compressed) throws IOException {
    final StringBuilder outStr = new StringBuilder();
    if ((compressed == null) || (compressed.length == 0)) {
      return "";
    }
    if (isCompressed(compressed)) {
      final GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed));
      final BufferedReader bufferedReader =
          new BufferedReader(new InputStreamReader(gis, Charset.defaultCharset()));
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        outStr.append(line);
      }
      gis.close();
    } else {
      outStr.append(compressed);
    }
    return outStr.toString();
  }

  protected boolean isCompressed(final byte[] compressed) {
    return (compressed[0] == (byte) (GZIPInputStream.GZIP_MAGIC))
        && (compressed[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));
  }
}
