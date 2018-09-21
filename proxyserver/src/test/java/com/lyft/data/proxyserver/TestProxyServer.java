package com.lyft.data.proxyserver;

import static org.testng.Assert.assertEquals;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import java.io.IOException;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.testng.annotations.Test;

public class TestProxyServer {

  @Test
  public void testProxyServer() throws IOException {
    String mockResponseText = "Test1234";
    int backendPort = 30000 + new Random().nextInt(1000);

    MockWebServer backend = new MockWebServer();
    backend.enqueue(new MockResponse().setBody(mockResponseText));
    backend.play(backendPort);

    int serverPort = backendPort + 1;
    ProxyServerConfiguration config = buildConfig(backend.getUrl("/").toString(), serverPort);
    ProxyServer proxyServer = new ProxyServer(config, new ProxyHandler());

    try {
      proxyServer.start();
      CloseableHttpClient httpclient = HttpClientBuilder.create().build();
      HttpUriRequest httpUriRequest = new HttpGet("http://localhost:" + serverPort);
      HttpResponse response = httpclient.execute(httpUriRequest);
      assertEquals(mockResponseText, EntityUtils.toString(response.getEntity()));
    } finally {
      proxyServer.close();
      backend.shutdown();
    }
  }

  @Test
  public void testCustomHeader() throws Exception {
    String mockResponseText = "CUSTOM HEADER TEST";
    int backendPort = 30000 + new Random().nextInt(1000);

    MockWebServer backend = new MockWebServer();
    backend.enqueue(new MockResponse().setBody(mockResponseText));
    backend.play(backendPort);

    int serverPort = backendPort + 1;
    ProxyServerConfiguration config = buildConfig(backend.getUrl("/").toString(), serverPort);
    ProxyServer proxyServer = new ProxyServer(config, new ProxyHandler());

    try {
      proxyServer.start();
      CloseableHttpClient httpclient = HttpClientBuilder.create().build();
      HttpUriRequest httpUriRequest = new HttpGet("http://localhost:" + serverPort);
      httpUriRequest.setHeader("HEADER1", "FOO");
      httpUriRequest.setHeader("HEADER2", "BAR");

      HttpResponse response = httpclient.execute(httpUriRequest);
      assertEquals(mockResponseText, EntityUtils.toString(response.getEntity()));
      RecordedRequest recordedRequest = backend.takeRequest();
      assertEquals(recordedRequest.getHeader("HEADER1"), "FOO");
      assertEquals(recordedRequest.getHeader("HEADER2"), "BAR");
    } finally {
      proxyServer.close();
      backend.shutdown();
    }
  }

  private ProxyServerConfiguration buildConfig(String backendUrl, int localPort) {
    ProxyServerConfiguration config = new ProxyServerConfiguration();
    config.setName("MockBackend");
    config.setPrefix("/");
    config.setPreserveHost("true");
    config.setProxyTo(backendUrl);
    config.setLocalPort(localPort);
    return config;
  }
}
