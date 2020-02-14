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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import com.lyft.data.proxyserver.wrapper.TenantAwareQueryAdapter;
import com.lyft.data.proxyserver.wrapper.TenantLookupService;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestSecurity {

    private static final String sampleTennantId = "e4010da4110ba377d100f050cb4440db";
    private static final String sampleAuthToken = "ce4d686756aa47f2b893b8602df67669";
   
    private MockTenantLookupService mockTenantLookupService;
    TenantAwareQueryAdapter adapter = new TenantAwareQueryAdapter();

    
    @BeforeSuite
    public void initLookupService() {
        mockTenantLookupService = new MockTenantLookupService();
        mockTenantLookupService.add(sampleAuthToken, sampleTennantId);
        adapter.setTenantLookupService(mockTenantLookupService);
    }
    
    @Test
    public void testTableAdapter() {
        String sql = "select * from mdmallergygolden where entityId='blah'";
        String bound = adapter.rewriteSql(sql, sampleAuthToken);
        assertEqualsFormattingStripped(bound,
                     "select * from e4010da4110ba377d100f050cb4440db_mdmallergygolden where (entityId = 'blah')");
    }
    
    //@Test - Geny this one's for you
    /*
    public void testShowTables() {
        TenantAwareQueryAdapter f = new TenantAwareQueryAdapter();
        String sql = "show tables";
        String bound = f.bindTables(sql, sampleAuthToken, mockTenantLookupService);
        assertEqualsFormattingStripped(bound, "show tables like 'e4010da4110ba377d100f050cb4440db%'");
    }
    
    public void testShowSchemas() {
        TenantAwareQueryAdapter f = new TenantAwareQueryAdapter();
        String sql = "show schemas";
        String bound = f.bindTables(sql, sampleAuthToken, mockTenantLookupService);
        assertEqualsFormattingStripped(bound, "show schemas like 'e4010da4110ba377d100f050cb4440db%'");
    }  
    
    public void testShowCatalogs() {
        TenantAwareQueryAdapter f = new TenantAwareQueryAdapter();
        String sql = "show schemas";
        String bound = f.bindTables(sql, sampleAuthToken, mockTenantLookupService);
        assertEqualsFormattingStripped(bound, "show catalogs like 'hive%'");
    }          
    
    // probably ShowCreate as well, check if there's anything else worth visiting & rewriting https://github.com/prestodb/presto/blob/master/presto-parser/src/main/java/com/facebook/presto/sql/tree/AstVisitor.java
         
    */   
    
    @Test
    public void testComplex() {
        String sql = "SELECT     receiptid, \n" + 
                "               p.productcode, \n" + 
                "               p.quantity, \n" + 
                "               p.tags,\n" + 
                "               products \n" + 
                "    FROM       recipts_compacted4 \n" + 
                "        CROSS JOIN Unnest(products) AS p(productcode, quantity, totalprice, normalizedproductdescription, unitofmeasuretax, tags)\n" + 
                "        CROSS JOIN Unnest(p.tags) AS tag(k,v) \n" + 
                "    WHERE      tag.k='brand' \n" + 
                "    AND        tag.v='cola' \n" + 
                "    AND        p.productcode IN ('813', '90002') limit 5000";
        
        List<String> bound = adapter.getTablesAccessed(sql, sampleAuthToken);
        assertEquals(bound.size(), 1);
        assertTrue(bound.contains("e4010da4110ba377d100f050cb4440db_recipts_compacted4"));
    }    

    @Test
    public void testJoin() {
        String sql = "select * from\n" + 
                "\n" + 
                "(select cast(order_date as date) as order_date,   \n" + 
                "        count(distinct(source_order_id)) as prim_orders, \n" + 
                "        sum(quantity) as prim_tickets, \n" + 
                "        sum(sale_amount) as prim_revenue \n" + 
                "from table_a\n" + 
                "where order_date >= date '2018-01-01'\n" + 
                "group by 1) as a\n" + 
                "\n" + 
                "left join\n" + 
                "\n" + 
                "(select summary_date, \n" + 
                "        sum(impressions) as sem_impressions, \n" + 
                "        sum(clicks) as sem_clicks, \n" + 
                "        sum(spend) as sem_spend,  \n" + 
                "        sum(total_orders) as sem_orders, \n" + 
                "        sum(total_tickets) as sem_tickets, \n" + 
                "        sum(total_revenue) as sem_revenue \n" + 
                "from table_b\n" + 
                "where site like '%SEM%'\n" + 
                "and summary_date >= date '2018-01-01'\n" + 
                "group by 1) as b\n" + 
                "\n" + 
                "on a.order_date = b.summary_date";
            
        List<String> bound = adapter.getTablesAccessed(sql, sampleAuthToken);
        assertEquals(bound.size(), 2);
        assertTrue(bound.contains("e4010da4110ba377d100f050cb4440db_table_a"));
        assertTrue(bound.contains("e4010da4110ba377d100f050cb4440db_table_b"));


    }    
    
    private void assertEqualsFormattingStripped(String sql1, String sql2) {
        
        assertEquals(sql1.replace("\n", " ").toLowerCase().replace("\r", " ").replaceAll(" +", " ").trim(),
                     sql2.replace("\n", " ").toLowerCase().replace("\r", " ").replaceAll(" +", " ").trim());
        
    }

    
    private class MockTenantLookupService implements TenantLookupService {
        private Map<String, String> authToTenantId = new HashMap<>();
        
        @Override
        public String getTenantId(String authToken) {
            return authToTenantId.get(authToken);
        }
        
        public void add(String authToken, String tenantId) {
            authToTenantId.put(authToken, tenantId);
        }
    }
}
