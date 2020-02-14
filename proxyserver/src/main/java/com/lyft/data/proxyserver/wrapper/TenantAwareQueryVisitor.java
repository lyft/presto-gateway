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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import com.facebook.presto.sql.tree.Cube;
import com.facebook.presto.sql.tree.DefaultTraversalVisitor;
import com.facebook.presto.sql.tree.DereferenceExpression;
import com.facebook.presto.sql.tree.GroupingSets;
import com.facebook.presto.sql.tree.Parameter;
import com.facebook.presto.sql.tree.QualifiedName;
import com.facebook.presto.sql.tree.Rollup;
import com.facebook.presto.sql.tree.ShowColumns;
import com.facebook.presto.sql.tree.Table;

public class TenantAwareQueryVisitor extends DefaultTraversalVisitor<Void, Void> {
    private String tenantId;
    private final List<String> tables = new ArrayList<>();
    
    public TenantAwareQueryVisitor(String tenantId) {
        super();
        this.tenantId = tenantId;
    }
    

    public List<String> getTables() {
        return tables;
    }

    @Override
    protected Void visitTable(Table node, Void context) {
        try {
            Field privateStringField = Table.class.getDeclaredField("name");
            privateStringField.setAccessible(true);
            String tenantSpecificTableName = tenantId + "_" + node.getName().toString();
            privateStringField.set(node, QualifiedName.of(tenantSpecificTableName));

        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        tables.add(node.getName().toString());
        
        return null;
    }
   
}
