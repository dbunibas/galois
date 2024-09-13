package galois.optimizer.estimators;

import galois.Constants;
import galois.llm.models.TogetherAIModel;
import galois.utils.Mapper;
import static galois.utils.Mapper.toCleanJsonList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.Attribute;
import speedy.model.database.IDatabase;
import speedy.model.database.ITable;
import speedy.utility.SpeedyUtility;

@Slf4j
public class ConfidenceEstimator {
    
    private String togetherAIModelName = TogetherAIModel.MODEL_LLAMA3_1_8B;
//    private String togetherAIModelName = TogetherAIModel.MODEL_LLAMA3_1_70B;
    
    public Map<ITable, Map<Attribute, Double>> getEstimation(IDatabase database) {
        String prompt = "Given the following relational schema ${relationalSchema} please give me your confidence, based on your internal knowledge, for every attribute. give me a value between 0 and 1, where 1 is perfect confidence and 0 is no confidence. Return the results in JSON with the properties \"attribute\" and \"confidence\"";
        Map<ITable, Map<Attribute, Double>> confidenceForDB = new HashMap<>();
        for (String tableName : database.getTableNames()) {
            ITable table = database.getTable(tableName);
            List<Attribute> attributes = table.getAttributes();
            String schema = getRelationalSchema(attributes,tableName);
            String promptTable = prompt.replace("${relationalSchema}", schema);
            if (log.isDebugEnabled()) log.debug(promptTable);
            TogetherAIModel model = new TogetherAIModel(Constants.TOGETHERAI_API, togetherAIModelName);
            String response = model.generate(promptTable);            
            if (log.isDebugEnabled()) log.debug(response);
            String cleanedResponse = toCleanJsonList(response);
            if (log.isDebugEnabled()) log.debug("Cleaned Response: {}", cleanedResponse);
            List<Map<String, Object>> fromJsonToListOfMaps = Mapper.fromJsonToListOfMaps(cleanedResponse);
            Map<Attribute, Double> confidenceForTable = new HashMap<>();
            for (Map<String, Object> json : fromJsonToListOfMaps) {
                String attributeName = json.get("attribute").toString();
                Double confidence = Double.valueOf(json.get("confidence").toString());
                Attribute attribute = findAttribute(attributes, attributeName);
                confidenceForTable.put(attribute, confidence);
            }
            confidenceForDB.put(table, confidenceForTable);
        }
        return confidenceForDB;
    }
    
    public Map<ITable, Map<Attribute, Double>> getEstimation(IDatabase database, List<String> tableNames, String querySQL) {
//        String prompt = "Given the following relational schema ${relationalSchema} and the query to execute over it ${query} please give me your confidence, based on your internal knowledge, for every attribute. give me a value between 0 and 1, where 1 is perfect confidence and 0 is no confidence.";
//        String prompt = "Given the following relational schema ${relationalSchema} and the query to execute over it ${query} please give me for every attribute of the schema your confidence in populating the values of the query results, based on your internal knowledge. Give me a value between 0.0 and 1.0, where 1.0 is perfect confidence and 0.0 is no confidence.";
        String prompt = "Given the following relational schema ${relationalSchema} and the query to execute over it ${query} please give me for every attribute of the schema your confidence in retrieving such data from your internal knowledge. Give me a value between 0.0 and 1.0, where 1.0 is perfect confidence and 0.0 is no confidence.";
//        String prompt = """
//                        Given the following relational schema ${relationalSchema} (assume the first attribute is the key) and the query to execute over it ${query} please tell me if I want to populate the values of the query results based on your internal knowledge, is it more reliable to extract tuple by tuple or first retrieve the key values and then the other attributes in follow up iterations. Answer with:
//                        
//                        1) Table for tuple by tuple;
//                        2) Key for key attribute first;
//                        3) It doesn't matter;
//                        """;
        Map<ITable, Map<Attribute, Double>> confidenceForDB = new HashMap<>();
        for (String tableName : tableNames) {
            ITable table = database.getTable(tableName);
            List<Attribute> attributes = table.getAttributes();
            String schema = getRelationalSchema(attributes,tableName);
            String promptTable = prompt.replace("${relationalSchema}", schema);
            promptTable = promptTable.replace("${query}", querySQL);
            if (log.isDebugEnabled()) log.debug(promptTable);
            TogetherAIModel model = new TogetherAIModel(Constants.TOGETHERAI_API, togetherAIModelName);
            String response = model.generate(promptTable);            
            if (log.isDebugEnabled()) log.debug(response);
        }
        return confidenceForDB;
    }
    
    private String getRelationalSchema(List<Attribute> attributes, String tableName) {
        String schema = tableName + "(";
        for (Attribute attribute : attributes) {
            if (attribute.getName().equalsIgnoreCase("oid")) continue;
            String name = attribute.getName();
            String type = attribute.getType();
            schema += type + " " + name + ", ";
        }
        schema = schema.substring(0, schema.length()-2);
        schema += ")";
        return schema;
    }
    
    private Attribute findAttribute(List<Attribute> attributes, String attributeName) {
        for (Attribute attribute : attributes) {
            if (attribute.getName().equalsIgnoreCase(attributeName)) return attribute;
        }
        return null;
    }
    
    

}
