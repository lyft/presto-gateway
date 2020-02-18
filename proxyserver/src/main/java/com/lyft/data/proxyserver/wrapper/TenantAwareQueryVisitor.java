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
import java.util.Optional;
import com.facebook.presto.sql.tree.*;

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

    /**
     * Swap out the table name with the full namespaced table name used in hive
     * customer -> [tenant UUID]_customer
     */
    @Override
    protected Void visitTable(Table node, Void context) {
        try {
            String tenantPrefix = tenantId + "_";
            String inputTableName = node.getName().getParts().get(node.getName().getParts().size() - 1);
            if(!inputTableName.startsWith(tenantPrefix)) {
                Field privateStringField = Table.class.getDeclaredField("name");
                privateStringField.setAccessible(true);
                String tenantSpecificTableName = tenantPrefix + node.getName().toString();
                privateStringField.set(node, QualifiedName.of(tenantSpecificTableName));
                
            }

        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new SecurityException("Unable to execute query");
        }
        tables.add(node.getName().toString());

        return null;
    }

    /**
     * BI tools like to show tables, make sure we prefix which ones it can see with a 'like' clause
     */
    @Override
    protected Void visitShowTables(ShowTables node, Void context) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(tenantId).append("_%");

            if (node.getLikePattern().isPresent()) {
                sb.append(node.getLikePattern().get());
            }
            Field privateStringField;
            privateStringField = ShowTables.class.getDeclaredField("likePattern");
            privateStringField.setAccessible(true);
            privateStringField.set(node, Optional.of(sb.toString()));
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new SecurityException("Unable to show tables");
        }

        return null;
    }

    protected Void visitShowSchemas(ShowSchemas node, Void context) {
        return null;
    }

    protected Void visitShowCatalogs(ShowCatalogs node, Void context) {
        //TODO: Fill in
        return null;
    }

    protected Void visitShowColumns(ShowColumns node, Void context) {
        //TODO: Is this covered by the table visitor? If not, implement
        return null;
    }

    protected Void visitShowStats(ShowStats node, Void context) {
        return null;
    }

    protected Void visitShowCreate(ShowCreate node, Void context) {
        //TODO: Is this covered by the table visitor? If not, implement        
        return null;
    }

    protected Void visitShowFunctions(ShowFunctions node, Void context) {
        return null;
    }

    protected Void visitUse(Use node, Void context) {
        //TODO: implement
        return null;
    }

    protected Void visitShowSession(ShowSession node, Void context) {
        //?, breakpoint and see if this even gets used by BI tooling
        return null;
    }

    protected Void visitSetSession(SetSession node, Void context) {
        //?, breakpoint and see if this even gets used by BI tooling
        return null;
    }

    protected Void visitResetSession(ResetSession node, Void context) {
        //?, breakpoint and see if this even gets used by BI tooling
        return null;
    }


    protected Void visitWithQuery(WithQuery node, Void context) {
        //?
        return null;
    }


    protected Void visitCreateSchema(CreateSchema node, Void context) {
        throw new SecurityException("Creating schemas not allowed");
    }

    protected Void visitDropSchema(DropSchema node, Void context) {
        throw new SecurityException("Drop schema not allowed");
    }

    protected Void visitRenameSchema(RenameSchema node, Void context) {
        throw new SecurityException("Renaming schemas not allowed");
    }

    protected Void visitCreateTable(CreateTable node, Void context) {
        throw new SecurityException("Create table not allowed");
    }

    protected Void visitCreateTableAsSelect(CreateTableAsSelect node, Void context) {
        throw new SecurityException("Create table not allowed");
    }



    protected Void visitDropTable(DropTable node, Void context) {
        throw new SecurityException("Drop table not allowed");
    }

    protected Void visitRenameTable(RenameTable node, Void context) {
        throw new SecurityException("Rename table not allowed");
    }

    protected Void visitRenameColumn(RenameColumn node, Void context) {
        throw new SecurityException("Rename column not allowed");
    }

    protected Void visitDropColumn(DropColumn node, Void context) {
        throw new SecurityException("Drop column not allowed");
    }

    protected Void visitAddColumn(AddColumn node, Void context) {
        throw new SecurityException("Add column not allowed");
    }

    protected Void visitAnalyze(Analyze node, Void context) {
        return visitStatement(node, context);
    }

    protected Void visitCreateView(CreateView node, Void context) {
        //TODO: This one's a maybe, starting off restricted until we can think it through
        throw new SecurityException("Create view not allowed");
    }

    protected Void visitDropView(DropView node, Void context) {
      //TODO: This one's a maybe, starting off restricted until we can think it through
        throw new SecurityException("Create view not allowed");    }

    protected Void visitCreateFunction(CreateFunction node, Void context) {
      //TODO: This one's a maybe, starting off restricted until we can think it through
        throw new SecurityException("Create view not allowed");
    }

    protected Void visitInsert(Insert node, Void context) {
        throw new SecurityException("Insert not allowed, use the REST API to modify/add records");
    }

    protected Void visitCall(Call node, Void context) {
        //?
        return null;
    }

    protected Void visitDelete(Delete node, Void context) {
        throw new SecurityException("Delete not allowed, use the REST API to modify/remove records");
    }

    protected Void visitStartTransaction(StartTransaction node, Void context) {
        return null;
    }

    protected Void visitCreateRole(CreateRole node, Void context) {
        throw new SecurityException("Create role not allowed");
    }

    protected Void visitDropRole(DropRole node, Void context) {
        throw new SecurityException("Drop role not allowed");
    }

    protected Void visitGrantRoles(GrantRoles node, Void context) {
        throw new SecurityException("Granting role not allowed");
    }

    protected Void visitRevokeRoles(RevokeRoles node, Void context) {
        throw new SecurityException("Revoke role not allowed");
    }

    protected Void visitSetRole(SetRole node, Void context) {
        throw new SecurityException("Setting role not allowed");
    }

    protected Void visitGrant(Grant node, Void context) {
        throw new SecurityException("Grant not allowed");
    }

    protected Void visitRevoke(Revoke node, Void context) {
        throw new SecurityException("Revoke not allowed");
    }

    protected Void visitShowGrants(ShowGrants node, Void context) {
        throw new SecurityException("List grants not allowed");
    }

    protected Void visitShowRoles(ShowRoles node, Void context) {
        throw new SecurityException("Show roles not allowed");
    }

    protected Void visitShowRoleGrants(ShowRoleGrants node, Void context) {
        throw new SecurityException("Show role grants not allowed");
    }


}
