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

import java.util.List;
import java.util.Optional;
import com.facebook.presto.sql.SqlFormatter;
import com.facebook.presto.sql.parser.ParsingOptions;
import com.facebook.presto.sql.parser.SqlParser;

public class TenantAwareQueryAdapter {
    public static final String V1_STATEMENT_PATH = "/v1/statement";

    // Inspired from
    // https://github.com/prestodb/presto/tree/master/presto-parser/src/test/java/com/facebook/presto/sql/parser

    private static final SqlParser SQL_PARSER = new SqlParser();
    private ParsingOptions parsingOptions = new ParsingOptions();
    private TenantLookupService tenantLookupService;

    public void setTenantLookupService(TenantLookupService tenantLookupService) {
        this.tenantLookupService = tenantLookupService;
    }


    public List<String> getTablesAccessed(String inputSql, String authHeader) {
        com.facebook.presto.sql.tree.Statement statement = SQL_PARSER.createStatement(inputSql, parsingOptions);
        TenantAwareQueryVisitor visitor = new TenantAwareQueryVisitor(tenantLookupService.getTenantId(authHeader));
        statement.accept(visitor, null);
        return visitor.getTables();
    }

    public String rewriteSql(String inputSql, String authHeader) {
        com.facebook.presto.sql.tree.Statement statement = SQL_PARSER.createStatement(inputSql, parsingOptions);
        TenantAwareQueryVisitor visitor = new TenantAwareQueryVisitor(tenantLookupService.getTenantId(authHeader));
        statement.accept(visitor, null);
        return SqlFormatter.formatSql(statement, Optional.empty());
    }

}
