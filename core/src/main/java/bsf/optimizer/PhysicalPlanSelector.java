package bsf.optimizer;

import bsf.parser.ParserProvenance;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import queryexecutor.model.database.AttributeRef;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.Key;

@Slf4j
public class PhysicalPlanSelector {
    
    public static final String PLAN_TABLE = "TABLE";
    public static final String PLAN_KEY_SCAN = "KEY_SCAN";
    
    //TODO return IQueryExecutor instance
    public String getPlanByKeyStrategy(IDatabase database, String sql) {
        List<Key> keys = database.getKeys();
        ParserProvenance parser = new ParserProvenance(database);
        parser.parse(sql);
        Set<String> attributeProvenance = parser.getAttributeProvenance();
        Set<String> tables = parser.getTablesProvenance();
        for (String attributeName : attributeProvenance) {
            for (Key key : keys) {
                List<AttributeRef> attributes = key.getAttributes();
                for (AttributeRef attribute : attributes) {
                    if (attribute.getName().equals(attributeName) && tables.contains(attribute.getTableName())) {
                        log.debug("Attribute: {} is key, return KEY_SCAN", attributeName);
                        return PLAN_KEY_SCAN;
                    }
                }
            }
        }
        log.debug("No attribute key found in query {}", sql);
        log.debug("Keys in DB: {}", keys);
        log.debug("Return TABLE");
        return PLAN_TABLE;
    }
    
}
