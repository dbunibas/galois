package engine.model.algebra;

import engine.model.algebra.operators.IAlgebraTreeVisitor;
import engine.model.algebra.operators.ITupleIterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.model.database.AttributeRef;
import engine.model.database.IDatabase;

public class CreateTableAs extends AbstractOperator {

    private static Logger logger = LoggerFactory.getLogger(CreateTableAs.class);

    private String tableName;
    private String tableAlias;
    private String schemaName;
    private boolean withOIDs;
    private boolean unlogged;

    public CreateTableAs(String tableName, String tableAlias, String schemaName, boolean withOIDs, boolean unlogged) {
        this.tableName = tableName;
        this.tableAlias = tableAlias;
        this.schemaName = schemaName;
        this.withOIDs = withOIDs;
        this.unlogged = unlogged;
    }

    public void accept(IAlgebraTreeVisitor visitor) {
        visitor.visitCreateTable(this);
    }

    public String getName() {
        return "CREATE " + (unlogged ? " UNLOGGED " : "") + "TABLE " + tableName + " AS ";
    }

    public ITupleIterator execute(IDatabase source, IDatabase target) {
        return this.getChildren().get(0).execute(source, target);
    }

    public List<AttributeRef> getAttributes(IDatabase source, IDatabase target) {
        return this.getChildren().get(0).getAttributes(source, target);
    }

    public String getTableName() {
        return tableName;
    }

    public String getTableAlias() {
        return tableAlias;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public boolean isWithOIDs() {
        return withOIDs;
    }

    public boolean isUnlogged() {
        return unlogged;
    }
}
