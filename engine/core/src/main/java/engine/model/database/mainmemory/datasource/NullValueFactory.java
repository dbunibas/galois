package engine.model.database.mainmemory.datasource;

import engine.EngineConstants;

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
        return EngineConstants.NULL_VALUE;
    }    
    
}

class GeneratedNullValue implements IDataSourceNullValue {
    
    public boolean isGenerated() {
        return true;
    }

    public String toString() {
        return EngineConstants.NULL_VALUE;
    }    
    
}