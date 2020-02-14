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

package com.lyft.data.proxyserver.wrapper;

import java.io.IOException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * To log into presto
 * 1) Log into carol
 * 2) Connectors UI, select one, hit the edit button
 * 3) Generate a token
 * 
 * When using presto, specify the user composed of [connector id]:[generated token]. Now you can log into presto like so
 * 
 *  presto-cli --server http://localhost:2233 --user 623cdc6dd7d343168805a47435d063e2:628c6f49379640b4bd1b0f1c33c9c346   --catalog hive --schema default
 * 
 * We validate your token against carol and if its good we'll return the tenantId so the query rewriter can modify the tables used for
 * this tenant's namespace. 
 * 
 * For example, at query time the TenantAwareQueryAdapter will use this service to grab the correct tenant ID and substitute the table name out 
 * in your query:
 * 
 *      select * from mdmallergygolden where entityId='blah'
 * becomes
 *      select * from e4010da4110ba377d100f050cb4440db_mdmallergygolden where entityId = 'blah'
 
 * 
 * @author drew
 *
 */
public class TenantLookupServiceImpl implements TenantLookupService {

    private String baseUrl;
    private HttpClient client = new DefaultHttpClient();

    public TenantLookupServiceImpl(String baseUrl) {
        super();
        this.baseUrl = baseUrl;
    }

    @Override
    public String getTenantId(String authToken) {
        String url = baseUrl + "api/v1/tenants/current" ;
        String[] pieces = authToken.split(":");
        if (pieces.length != 2) {
            throw new SecurityException("Unable to login using provided token");
        }

        HttpGet request = new HttpGet(url);
        try {
            request.addHeader("X-Auth-ConnectorId", pieces[0]);
            request.addHeader("X-Auth-Key", pieces[1]);
            request.addHeader("accept", "application/json");
            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            Integer statusCode = response.getStatusLine().getStatusCode();
            if(statusCode > 201 && statusCode < 500) {
                throw new SecurityException("Invalid authentication token");
            } else if (statusCode >= 500) {
                throw new SecurityException("Failure authenticating with carol API, status code " + statusCode);
            }
            
            String content = EntityUtils.toString(entity);
            JsonElement je = new JsonParser().parse(content);
            return je.getAsJsonObject().get("mdmTenantId").getAsString();
        } catch (IOException e) {
            throw new SecurityException(e);
        } finally {
            request.releaseConnection();    
        }
    }

}
