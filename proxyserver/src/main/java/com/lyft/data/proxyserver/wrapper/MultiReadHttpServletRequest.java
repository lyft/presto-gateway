package com.lyft.data.proxyserver.wrapper;

import java.io.*;
import java.util.*;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.eclipse.jetty.http.HttpHeader;

public class MultiReadHttpServletRequest extends HttpServletRequestWrapper {
  private byte[] content;
  private final Map<String, String> headerMap = new HashMap<>();

  public static void copy(InputStream in, OutputStream out) throws IOException {

    byte[] buffer = new byte[1024];
    while (true) {
      int bytesRead = in.read(buffer);
      if (bytesRead == -1) {
        break;
      }
      out.write(buffer, 0, bytesRead);
    }
  }

  public MultiReadHttpServletRequest(HttpServletRequest request) throws IOException {
    super(request);
    this.storeBody(request.getInputStream());
  }

  private void storeBody(InputStream inputStream) throws IOException {
    ByteArrayOutputStream bodyInOutputStream = new ByteArrayOutputStream();
    copy(inputStream, bodyInOutputStream);
    this.content = bodyInOutputStream.toByteArray();
  }

  /**
   * add a header with given name and value.
   *
   * @param name
   * @param value
   */
  public void addHeader(String name, String value) {
    headerMap.put(name, value);
  }

  public void replaceBody(String newBody) throws IOException {
    this.storeBody(new ByteArrayInputStream(newBody.getBytes()));

    // Also update the content-length
    Integer contentLength = newBody.getBytes(getCharacterEncoding()).length;
    addHeader(HttpHeader.CONTENT_LENGTH.asString(), contentLength.toString());
  }

  @Override
  public String getHeader(String name) {
    String headerValue = super.getHeader(name);
    if (headerMap.containsKey(name)) {
      headerValue = headerMap.get(name);
    }
    return headerValue;
  }

  /**
   * get the header names.
   */
  @Override
  public Enumeration<String> getHeaderNames() {
    Set<String> names = new HashSet<>(Collections.list(super.getHeaderNames()));
    for (String name : headerMap.keySet()) {
      names.add(name);
    }
    return Collections.enumeration(names);
  }

  @Override
  public Enumeration<String> getHeaders(String name) {
    Set<String> values = new HashSet<>(Collections.list(super.getHeaders(name)));
    if (headerMap.containsKey(name)) {
      values.add(headerMap.get(name));
    }
    return Collections.enumeration(values);
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
    final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content);
    return new ServletInputStream() {
      @Override
      public boolean isFinished() {
        return false;
      }

      @Override
      public boolean isReady() {
        return false;
      }

      @Override
      public void setReadListener(ReadListener readListener) {}

      public int read() throws IOException {
        return byteArrayInputStream.read();
      }
    };
  }

  @Override
  public BufferedReader getReader() throws IOException {
    return new BufferedReader(new InputStreamReader(this.getInputStream()));
  }
}
