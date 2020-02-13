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
    private String authHeader;
    private TenantLookupService tenantLookupService;
    private final List<String> tables = new ArrayList<>();
    
    public TenantAwareQueryVisitor(String authHeader, TenantLookupService tenantLookupService) {
        super();
        this.authHeader = authHeader;
        this.tenantLookupService = tenantLookupService;
    }
    

    public List<String> getTables() {
        return tables;
    }

    @Override
    public Void visitParameter(Parameter node, Void context) {
        //parameters.add(node);
        return null;
    }
    
    @Override
    protected Void visitShowColumns(ShowColumns node, Void context) {
        //node.setTable(adaptTableName(node.getTable()));
        return null;
    }
    
    @Override
    protected Void visitTable(Table node, Void context) {
        try {
            Field privateStringField = Table.class.getDeclaredField("name");
            privateStringField.setAccessible(true);
            String tenantSpecificTableName = this.tenantLookupService.getTenantId(authHeader) + "_" + node.getName().toString();
            privateStringField.set(node, QualifiedName.of(tenantSpecificTableName));

        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        tables.add(node.getName().toString());
        
        return null;
    }
    
    @Override
    protected Void visitCube(Cube node, Void context) {
        //node.setColumns(adaptQualifiedNames(node.getColumns()));
        return null;
    }
    
    @Override
    protected Void visitGroupingSets(GroupingSets node, Void context) {
        //node.setSets(node.getSets().stream().map(this::adaptQualifiedNames).collect(toList()));
        return null;
    }
    
    @Override
    protected Void visitRollup(Rollup node, Void context) {
        //node.setColumns(adaptQualifiedNames(node.getColumns()));
        return null;
    }        
    
    @Override
    protected Void visitDereferenceExpression(DereferenceExpression node, Void context) {
        QualifiedName parsedName = DereferenceExpression.getQualifiedName(node);
        System.out.println(parsedName);
        return null;
    }
    
    private List<QualifiedName> adaptQualifiedNames(List<QualifiedName> names) {
        //return names.stream().map(this::adaptQualifiedName).collect(toList());
        return null;
    }
    
    private QualifiedName adaptQualifiedName(QualifiedName name) {
        List<String> parts = name.getParts();
        if (parts.size() == 1) {
            return name;
        }
        return QualifiedName.of("blah");
    }     
    
    private QualifiedName adaptTableName(QualifiedName name) {
        return QualifiedName.of("blah");
    }     
}
