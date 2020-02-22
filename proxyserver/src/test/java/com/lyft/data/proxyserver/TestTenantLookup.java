/**
 * TOTVS LABS CONFIDENTIAL
 * _______________________
 *
 *  2012-2016 (c) TOTVS Labs
 *  All Rights Reserved.
 *
 *  NOTICE:  All information contained herein is, and remains the
 *  property of TOTVS Labs and its suppliers, if any. The intellectual
 *  and technical concepts contained herein are proprietary to TOTVS Labs
 *  and its suppliers and may be covered by U.S. and Foreign Patents,
 *  patents in process, and are protected  by trade secret or copyright
 *  law. Dissemination of this information or reproduction of this material
 *  is strictly forbidden unless prior written permission is obtained
 *  from TOTVS Labs.
 */

package com.lyft.data.proxyserver;

import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.mockito.Mockito;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import com.lyft.data.proxyserver.wrapper.TenantId;
import com.lyft.data.proxyserver.wrapper.TenantLookupServiceImpl;
import static org.testng.Assert.assertEquals;

public class TestTenantLookup {

    private TenantLookupServiceImpl lookup; 
    private static HttpClient httpClient;
    private static HttpResponse httpResponse;
    private static StatusLine statusLine;
    
    @BeforeTest
    public void init() {
        httpClient = Mockito.mock(HttpClient.class);
        httpResponse = Mockito.mock(HttpResponse.class);
        statusLine = Mockito.mock(StatusLine.class);
        lookup = new TenantLookupServiceImpl(httpClient, "https://karol.ai/");
    }
    
    @Test
    public void testLookup() throws ClientProtocolException, IOException {
        Mockito.when(statusLine.getStatusCode()).thenReturn(200);
        Mockito.when(httpResponse.getStatusLine()).thenReturn(statusLine);
        Mockito.when(httpResponse.getEntity()).thenReturn(new StringEntity("{\n" + 
                "  \"mdmAllowSmsLogin\": true,\n" + 
                "  \"mdmAllowSmsUsage\": true,\n" + 
                "  \"mdmCreated\": \"2020-01-08T19:36:15.381Z\",\n" + 
                "  \"mdmCreatedUser\": \"drew@totvs.com\",\n" + 
                "  \"mdmDescription\": {\n" + 
                "  },\n" + 
                "  \"mdmEnableAddressCleansing\": false,\n" + 
                "  \"mdmEnableSaml\": false,\n" + 
                "  \"mdmEntityType\": \"mdmTenant\",\n" + 
                "  \"mdmExternalLoginConfiguration\": {\n" + 
                "  },\n" + 
                "  \"mdmId\": \"dec151c9f6aa4deca1b6e76cb7190411\",\n" + 
                "  \"mdmIsPublicData\": false,\n" + 
                "  \"mdmLabel\": {\n" + 
                "    \"en-US\": \"drew\"\n" + 
                "  },\n" + 
                "  \"mdmLastUpdated\": \"2020-01-08T19:37:31.141Z\",\n" + 
                "  \"mdmLocale\": \"en-US\",\n" + 
                "  \"mdmMappingsPending\": false,\n" + 
                "  \"mdmName\": \"drew\",\n" + 
                "  \"mdmOrgId\": \"3fcd5d006318447096527b5f52e9fc6f\",\n" + 
                "  \"mdmProcessingPriority\": 0,\n" + 
                "  \"mdmQueueSuffix\": \"\",\n" + 
                "  \"mdmSendEmailSmsCode\": false,\n" + 
                "  \"mdmSmsProvider\": \"TWW\",\n" + 
                "  \"mdmStatus\": \"ACTIVE\",\n" + 
                "  \"mdmStatusDate\": \"2020-01-08T19:37:31.141Z\",\n" + 
                "  \"mdmStopProcessing\": false,\n" + 
                "  \"mdmSubdomain\": \"drew\",\n" + 
                "  \"mdmTag\": \"PRODUCTION\",\n" + 
                "  \"mdmTenantId\": \"8cd6e43115e9416eb23609486fa053e3\",\n" + 
                "  \"mdmUpdatedUser\": \"\",\n" + 
                "  \"mdmWhiteListDomains\": [\n" + 
                "    \"*\"\n" + 
                "  ],\n" + 
                "  \"tenantType\": \"DEVELOPMENT\"\n" + 
                "}", "utf-8"));
        
        Mockito.when(httpClient.execute(Mockito.any(HttpGet.class))).thenReturn(httpResponse);
        TenantId tenantId = lookup.getTenantId("623cdc6dd7d343168805a47435d063e2_628c6f49379640b4bd1b0f1c33c9c346");
        assertEquals(tenantId.get(), "8cd6e43115e9416eb23609486fa053e3");
    }

    @Test(expectedExceptions = SecurityException.class)
    public void testBadInput() throws ClientProtocolException, IOException {
        Mockito.when(statusLine.getStatusCode()).thenReturn(400);
        Mockito.when(httpResponse.getStatusLine()).thenReturn(statusLine);
        Mockito.when(httpClient.execute(Mockito.any(HttpGet.class))).thenReturn(httpResponse);
        TenantId tenantId = lookup.getTenantId("623cdc6dd7d343168805a47435d063e2_628c6f49379640b4bd1b0f1c33c9c346");
    }
    
}
