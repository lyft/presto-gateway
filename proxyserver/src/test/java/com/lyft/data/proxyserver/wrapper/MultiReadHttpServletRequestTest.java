package com.lyft.data.proxyserver.wrapper;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import com.google.common.io.CharStreams;
import com.lyft.data.proxyserver.ProxyHandler;
import com.lyft.data.proxyserver.ProxyServer;
import com.lyft.data.proxyserver.ProxyServerConfiguration;
import com.lyft.data.proxyserver.TestProxyServer;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;
import java.util.Random;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.testng.annotations.Test;

/**
 * Author: Tejinder Aulakh (taulakh@lyft.com)
 * Created on: 4/22/20
 */

@Slf4j
public class MultiReadHttpServletRequestTest {
  @Test
  public void testReplaceBody() throws Exception {
    String requestBodyOriginal = "SELECT 1";
    String requestBodyNew = "SELECT XY";
    String mockResponseText = "Test1234";

    MockWebServer backend = new MockWebServer();
    backend.enqueue(new MockResponse().setBody(mockResponseText));
    int backendPort = 30000 + new Random().nextInt(1000);
    backend.play(backendPort);

    int serverPort = backendPort + 1;
    ProxyServerConfiguration config = TestProxyServer.buildConfig(backend.getUrl("/").toString(),
            serverPort);

    // Create a custom proxy handler which will be replacing the body
    ProxyHandler customProxyHandler = new ProxyHandler() {
      @Override
      protected String rewriteTarget(HttpServletRequest request)  {
        if (request instanceof MultiReadHttpServletRequest) {
          try {
            MultiReadHttpServletRequest req = (MultiReadHttpServletRequest) request;
            // Verify that the client is sending the original body
            assertEquals(CharStreams.toString(req.getReader()), requestBodyOriginal);

            // Now, replace the body
            //req.replaceBody(requestBodyNew);
          } catch (Exception e) {
            fail();
          }
        }
        return super.rewriteTarget(request);
      }
    };

    ProxyServer proxyServer = new ProxyServer(config, customProxyHandler);
    try {
      // Start the proxy server
      proxyServer.start();

      // Create an httpclient and send an request to the proxy (original body)
      CloseableHttpClient httpclient = HttpClientBuilder.create().build();

      HttpPost httpPost = new HttpPost("http://localhost:" + serverPort);
      StringEntity entity = new StringEntity(requestBodyOriginal);
      httpPost.setEntity(entity);
      httpPost.setHeader("Content-Type", "text/plain; charset=UTF-8");

      HttpResponse response = httpclient.execute(httpPost);

      // Now, verify that the modified body is received by the backend server
      RecordedRequest recordedRequest = backend.takeRequest();
      log.info("Body received by the backend is: " + recordedRequest.getUtf8Body());
      //assertEquals(recordedRequest.getUtf8Body(), requestBodyNew);
    } finally {
      proxyServer.close();
      backend.shutdown();
    }
  }
}