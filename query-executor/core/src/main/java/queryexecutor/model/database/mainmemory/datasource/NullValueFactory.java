package queryexecutor.model.database.mainmemory.datasource;

import queryexecutor.QueryExecutorConstants;

public class NullValueFactory {
    
    private static IDataSourceNullValue nullValue = new NullValue();
    private static IDataSourceNullValue generatedNullValue = new GeneratedNullValue();
    
    public static IDataSourceNullValue getNullValue() {
        return nullValue;
    }
    
    public static IDataSourceNullValue getGeneratedNullValue() {
        return generatedNullValue;
    }

    private NullValueFactory() {}
        
}

class NullValue implements IDataSourceNullValue {
    
    public boolean isGenerated() {
        return false;
    }
    
    public String toString() {
        return QueryExecutorConstants.NULL_VALUE;
    }    
    
}

class GeneratedNullValue implements IDataSourceNullValue {
    
    public boolean isGenerated() {
        return true;
    }

    public String toString() {
        return QueryExecutorConstants.NULL_VALUE;
    }    
    
}