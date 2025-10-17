package queryexecutor.model.database;

public class Attribute {

    private String tableName;
    private String name;
    private String type;
    private Boolean nullable;

    public Attribute(String tableName, String name, String type) {
        this.tableName = tableName;
        this.name = name;
        this.type = type;
    }

    public Attribute(String tableName, String name, String type, Boolean nullable) {
        this.tableName = tableName;
        this.name = name;
        this.type = type;
        this.nullable = nullable;
    }

    public String getName() {
        return name;
    }

    public String getTableName() {
        return tableName;
    }

    public String getType() {
        return type;
    }

    public Boolean getNullable() {
        return nullable;
    }

    public void setNullable(Boolean nullable) {
        this.nullable = nullable;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (this.tableName != null ? this.tableName.hashCode() : 0);
        hash = 97 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final Attribute other = (Attribute) obj;
        if ((this.tableName == null) ? (other.tableName != null) : !this.tableName.equals(other.tableName)) return false;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) return false;
        return true;
    }
    
    

    @Override
    public String toString() {
        return "Attribute{" + "tableName=" + tableName + ", name=" + name + ", type=" + type + '}';
    }

    
}
