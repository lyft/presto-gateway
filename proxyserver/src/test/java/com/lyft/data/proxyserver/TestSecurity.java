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
import com.lyft.data.proxyserver.wrapper.TenantId;
import com.lyft.data.proxyserver.wrapper.TenantLookupService;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestSecurity {

    private static final TenantId authenticatedTennantId = new TenantId("e4010da4110ba377d100f050cb4440db");
    private static final String sampleAuthToken = "ce4d686756aa47f2b893b8602df67669";
   
    private MockTenantLookupService mockTenantLookupService;
    TenantAwareQueryAdapter adapter = new TenantAwareQueryAdapter();

    
    @BeforeSuite
    public void initLookupService() {
        mockTenantLookupService = new MockTenantLookupService();
        mockTenantLookupService.add(sampleAuthToken, authenticatedTennantId);
        adapter.setTenantLookupService(mockTenantLookupService);
    }
    
    @Test
    public void testTableAdapter() {
        String sql = "select * from mdmallergygolden where entityId='blah'";
        String bound = adapter.rewriteSql(sql, authenticatedTennantId);
        assertEqualsFormattingStripped(bound,
                     "select * from e4010da4110ba377d100f050cb4440db_mdmallergygolden where (entityId = 'blah')");
    }
    @Test
    public void testTableAlreadyNamespaced() {
        String sql = "select * from e4010da4110ba377d100f050cb4440db_mdmallergygolden where entityId='blah'";
        String bound = adapter.rewriteSql(sql, authenticatedTennantId);
        assertEqualsFormattingStripped(bound,
                     "select * from e4010da4110ba377d100f050cb4440db_mdmallergygolden where (entityId = 'blah')");
    }
    
    @Test
    public void testSupersetShowColumns() {
        String sql = "SHOW COLUMNS FROM \"default\".\"8cd6e43115e9416eb23609486fa053e3_recipts\"";
        String bound = adapter.rewriteSql(sql, authenticatedTennantId);
        assertEqualsFormattingStripped(bound,
                "SHOW COLUMNS FROM default.\"8cd6e43115e9416eb23609486fa053e3_recipts\"");
        
    }
    
    @Test
    public void testPreiods () {
        String sql = "SELECT \"receiptid\" AS \"receiptid\",\n" + 
                "       \"datetimeemission\" AS \"datetimeemission\",\n" + 
                "       \"mdmtaxid\" AS \"mdmtaxid\",\n" + 
                "       \"mdmstatetaxid\" AS \"mdmstatetaxid\",\n" + 
                "       \"mdmcompanyname\" AS \"mdmcompanyname\",\n" + 
                "       \"mdmdba\" AS \"mdmdba\",\n" + 
                "       \"cnae\" AS \"cnae\",\n" + 
                "       \"mdmaddress\" AS \"mdmaddress\",\n" + 
                "       \"additionalinformatio\" AS \"additionalinformatio\",\n" + 
                "       \"customerobj\" AS \"customerobj\",\n" + 
                "       \"products\" AS \"products\",\n" + 
                "       \"originalcnae\" AS \"originalcnae\",\n" + 
                "       \"environment\" AS \"environment\",\n" + 
                "       \"mdmcounterforentity\" AS \"mdmcounterforentity\",\n" + 
                "       \"mdmid\" AS \"mdmid\",\n" + 
                "       \"mdmcreated\" AS \"mdmcreated\",\n" + 
                "       \"mdmlastupdated\" AS \"mdmlastupdated\",\n" + 
                "       \"mdmtenantid\" AS \"mdmtenantid\",\n" + 
                "       \"mdmentitytype\" AS \"mdmentitytype\",\n" + 
                "       \"mdmsourceentitynames\" AS \"mdmsourceentitynames\",\n" + 
                "       \"mdmcrosswalk\" AS \"mdmcrosswalk\",\n" + 
                "       \"mdmapplicationidmasterrecordid\" AS \"mdmapplicationidmasterrecordid\"\n" + 
                "FROM \"default\".\"recipts\"\n" + 
                "LIMIT 100";
    }
    
    @Test
    public void testSupersetDataPreviewQuery() {
        String sql = "SELECT \"receiptid\" AS \"receiptid\",\n" + 
                "       \"datetimeemission\" AS \"datetimeemission\",\n" + 
                "       \"mdmtaxid\" AS \"mdmtaxid\",\n" + 
                "       \"mdmstatetaxid\" AS \"mdmstatetaxid\",\n" + 
                "       \"mdmcompanyname\" AS \"mdmcompanyname\",\n" + 
                "       \"mdmdba\" AS \"mdmdba\",\n" + 
                "       \"cnae\" AS \"cnae\",\n" + 
                "       \"mdmaddress\" AS \"mdmaddress\",\n" + 
                "       \"additionalinformatio\" AS \"additionalinformatio\",\n" + 
                "       \"customerobj\" AS \"customerobj\",\n" + 
                "       \"products\" AS \"products\",\n" + 
                "       \"originalcnae\" AS \"originalcnae\",\n" + 
                "       \"environment\" AS \"environment\",\n" + 
                "       \"mdmcounterforentity\" AS \"mdmcounterforentity\",\n" + 
                "       \"mdmid\" AS \"mdmid\",\n" + 
                "       \"mdmcreated\" AS \"mdmcreated\",\n" + 
                "       \"mdmlastupdated\" AS \"mdmlastupdated\",\n" + 
                "       \"mdmtenantid\" AS \"mdmtenantid\",\n" + 
                "       \"mdmentitytype\" AS \"mdmentitytype\",\n" + 
                "       \"mdmsourceentitynames\" AS \"mdmsourceentitynames\",\n" + 
                "       \"mdmcrosswalk\" AS \"mdmcrosswalk\",\n" + 
                "       \"mdmapplicationidmasterrecordid\" AS \"mdmapplicationidmasterrecordid\"\n" + 
                "FROM \"default\".\"8cd6e43115e9416eb23609486fa053e3_recipts\"\n" + 
                "LIMIT 100";
        String bound = adapter.rewriteSql(sql, authenticatedTennantId);
        assertEqualsFormattingStripped(bound, "select \"receiptid\" \"receiptid\" , \"datetimeemission\" \"datetimeemission\" , \"mdmtaxid\" \"mdmtaxid\" , \"mdmstatetaxid\" \"mdmstatetaxid\" , \"mdmcompanyname\" \"mdmcompanyname\" , \"mdmdba\" \"mdmdba\" , \"cnae\" \"cnae\" , \"mdmaddress\" \"mdmaddress\" , \"additionalinformatio\" \"additionalinformatio\" , \"customerobj\" \"customerobj\" , \"products\" \"products\" , \"originalcnae\" \"originalcnae\" , \"environment\" \"environment\" , \"mdmcounterforentity\" \"mdmcounterforentity\" , \"mdmid\" \"mdmid\" , \"mdmcreated\" \"mdmcreated\" , \"mdmlastupdated\" \"mdmlastupdated\" , \"mdmtenantid\" \"mdmtenantid\" , \"mdmentitytype\" \"mdmentitytype\" , \"mdmsourceentitynames\" \"mdmsourceentitynames\" , \"mdmcrosswalk\" \"mdmcrosswalk\" , \"mdmapplicationidmasterrecordid\" \"mdmapplicationidmasterrecordid\" from \"e4010da4110ba377d100f050cb4440db_default.8cd6e43115e9416eb23609486fa053e3_recipts\" limit 100");
        
    }

    
    @Test
    public void testShowTables() {
        String sql = "show tables";
        String bound = adapter.rewriteSql(sql, authenticatedTennantId);
        assertEqualsFormattingStripped(bound, "show tables like 'e4010da4110ba377d100f050cb4440db_%'");
    }
    
    @Test
    public void testShowTablesModify() {
        String sql = "show tables like 'recipt%'";
        String bound = adapter.rewriteSql(sql, authenticatedTennantId);
        assertEqualsFormattingStripped(bound, "show tables like 'e4010da4110ba377d100f050cb4440db_%recipt%'");
    }    

    //@Test - Geny this one's for you
    /*
    
    public void testShowSchemas() {
        String sql = "show schemas";
        String bound = adapter.rewriteSql(sql, sampleAuthToken);
        assertEqualsFormattingStripped(bound, "show schemas like 'e4010da4110ba377d100f050cb4440db%'");
    }  
    
    public void testShowCatalogs() {
        String sql = "show schemas";
        String bound = adapter.rewriteSql(sql, sampleAuthToken);
        assertEqualsFormattingStripped(bound, "show catalogs like 'hive%'");
    }          
    
    // probably ShowCreate as well, check if there's anything else worth visiting & rewriting https://github.com/prestodb/presto/blob/master/presto-parser/src/main/java/com/facebook/presto/sql/tree/AstVisitor.java
     * 
         
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
        
        List<String> bound = adapter.getTablesAccessed(sql, authenticatedTennantId);
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
            
        List<String> bound = adapter.getTablesAccessed(sql, authenticatedTennantId);
        assertEquals(bound.size(), 2);
        assertTrue(bound.contains("e4010da4110ba377d100f050cb4440db_table_a"));
        assertTrue(bound.contains("e4010da4110ba377d100f050cb4440db_table_b"));


    }    
    
    private void assertEqualsFormattingStripped(String sql1, String sql2) {
        
        assertEquals(sql1.replace("\n", " ").toLowerCase().replace("\r", " ").replaceAll(" +", " ").trim(),
                     sql2.replace("\n", " ").toLowerCase().replace("\r", " ").replaceAll(" +", " ").trim());
        
    }

    
    private class MockTenantLookupService implements TenantLookupService {
        private Map<String, TenantId> authToTenantId = new HashMap<>();
        
        @Override
        public TenantId getTenantId(String authToken) {
            return authToTenantId.get(authToken);
        }
        
        public void add(String authToken, TenantId tenantId) {
            authToTenantId.put(authToken, tenantId);
        }
    }
}
