package com.lyft.data.proxyserver;

import com.lyft.data.proxyserver.wrapper.MultiReadHttpServletRequest;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RequestFilter implements Filter {
  private FilterConfig filterConfig = null;

  public void init(FilterConfig filterConfig) throws ServletException {
    this.filterConfig = filterConfig;
  }

  public void destroy() {
    this.filterConfig = null;
  }

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    // We need to convert the ServletRequest to MultiReadRequest, so that we can intercept later
    MultiReadHttpServletRequest multiReadRequest =
        new MultiReadHttpServletRequest((HttpServletRequest) request);
    HttpServletResponseWrapper responseWrapper =
        new HttpServletResponseWrapper((HttpServletResponse) response);
    chain.doFilter(multiReadRequest, responseWrapper);
  }
}
