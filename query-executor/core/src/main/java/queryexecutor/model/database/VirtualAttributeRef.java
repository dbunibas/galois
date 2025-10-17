package queryexecutor.model.database;

public class VirtualAttributeRef extends AttributeRef {

    private final String type;

    public VirtualAttributeRef(TableAlias tableAlias, String name, String type) {
        super(tableAlias, name);
        this.type = type;
    }

    public VirtualAttributeRef(String tableName, String name, String type) {
        super(tableName, name);
        this.type = type;
    }

    public VirtualAttributeRef(AttributeRef originalRef, TableAlias newAlias, String type) {
        super(originalRef, newAlias);
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
