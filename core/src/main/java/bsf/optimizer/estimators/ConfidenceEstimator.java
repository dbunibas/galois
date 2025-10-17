package bsf.optimizer.estimators;

import dev.langchain4j.model.chat.ChatLanguageModel;
import bsf.Constants;
import bsf.llm.models.TogetherAIModel;
import bsf.llm.models.togetherai.TogetherAIConstants;
import bsf.llm.query.ConversationalChainFactory;
import bsf.parser.ParserProvenance;
import bsf.parser.ParserWhere;
import bsf.utils.Mapper;
import lombok.extern.slf4j.Slf4j;
import queryexecutor.model.database.Attribute;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.ITable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static bsf.utils.Mapper.toCleanJsonList;
import java.util.HashSet;
import java.util.Set;
import queryexecutor.model.database.AttributeRef;
import queryexecutor.model.database.Key;
import net.sf.jsqlparser.expression.Expression;

@Slf4j
public class ConfidenceEstimator {
    
  private String togetherAIModelName = Constants.TOGETHERAI_MODEL;
    
  public Map<ITable, Map<Attribute, Double>> getEstimation(IDatabase database) {
//        String prompt = "Given the following relational schema ${relationalSchema} please give me your confidence, based on your internal knowledge, for every attribute. give me a value between 0 and 1, where 1 is perfect confidence and 0 is no confidence. Return the results in JSON with the properties \"attribute\" and \"confidence\"";
        String prompt = "Given the following relational schema ${relationalSchema} please give me your confidence, based on your internal knowledge, for every attribute. give me a value between 0 and 1, where 1 is perfect confidence and 0 is no confidence. Return an higher score if you are able to return accurate and factual data. Return a lower score if you can't return accurate and factual data. Return the results in JSON with the properties \"attribute\" and \"confidence\"";
        Map<ITable, Map<Attribute, Double>> confidenceForDB = new HashMap<>();
        for (String tableName : database.getTableNames()) {
            ITable table = database.getTable(tableName);
            List<Attribute> attributes = table.getAttributes();
            String schema = getRelationalSchema(attributes,tableName);
            String promptTable = prompt.replace("${relationalSchema}", schema);
            if (log.isDebugEnabled()) log.debug(promptTable);
            ChatLanguageModel model = new TogetherAIModel(Constants.TOGETHERAI_API, togetherAIModelName, TogetherAIConstants.STREAM_MODE);
            if (Constants.LLM_MODEL.equals(Constants.MODEL_GPT)) model = ConversationalChainFactory.buildOpenAIChatLanguageModel(Constants.OPEN_AI_API_KEY, Constants.OPEN_AI_CHAT_MODEL_NAME);
            String response = model.generate(promptTable);            
            if (log.isDebugEnabled()) log.debug(response);
            String cleanedResponse = toCleanJsonList(response, true);
            if (log.isDebugEnabled()) log.debug("Cleaned Response: {}", cleanedResponse);
            List<Map<String, Object>> fromJsonToListOfMaps = Mapper.fromJsonToListOfMaps(cleanedResponse, true);
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
    
    public Map<ITable, Double> getEstimationSchema(IDatabase database) {
//        String prompt = "Given the following relational schema ${relationalSchema} please give me your confidence, based on your internal knowledge, for every attribute. give me a value between 0 and 1, where 1 is perfect confidence and 0 is no confidence. Return the results in JSON with the properties \"attribute\" and \"confidence\"";
        String prompt = "Given the following relational schema ${relationalSchema} please give me your confidence in popolating such schema based on your internal knowledge. Give me a value between 0 and 1, where 1 is perfect confidence and 0 is no confidence. Return an higher score if you are able to return accurate and factual data. Return a lower score if you can't return accurate and factual data. Return the result in JSON with the property \"confidence\"";
        Map<ITable, Double> confidenceForDB = new HashMap<>();
        for (String tableName : database.getTableNames()) {
            ITable table = database.getTable(tableName);
            List<Attribute> attributes = table.getAttributes();
            String schema = getRelationalSchema(attributes,tableName);
            String promptTable = prompt.replace("${relationalSchema}", schema);
            if (log.isDebugEnabled()) log.debug(promptTable);
            ChatLanguageModel model = new TogetherAIModel(Constants.TOGETHERAI_API, togetherAIModelName, TogetherAIConstants.STREAM_MODE);
            if (Constants.LLM_MODEL.equals(Constants.MODEL_GPT)) model = ConversationalChainFactory.buildOpenAIChatLanguageModel(Constants.OPEN_AI_API_KEY, Constants.OPEN_AI_CHAT_MODEL_NAME);
            String response = model.generate(promptTable);            
            if (log.isDebugEnabled()) log.debug(response);
            String cleanedResponse = Mapper.toCleanJsonObject(response);
            if (log.isDebugEnabled()) log.debug("Cleaned Response: {}", cleanedResponse);

        }
        return confidenceForDB;
    }
    
    public void getEstimationForQuery(IDatabase database, List<String> tableNames, String querySQL) {
        /*String prompt = "Given the following relational schema ${relationalSchema} please give me your confidence, based on your internal knowledge, in populating the data given a SQL query. Give me a value between 0 and 1, where 1 is perfect confidence and 0 is no confidence. Return a higher score if you are able to return accurate and factual data. Return a lower score if you can't return accurate and factual data or you may not have information on all the data. Return the results in JSON with the property \"confidence\".\n"
                + "\n"
                + "SQL query: ${sqlQuery}"; */
        String prompt = "Given the following relational schema ${relationalSchema} please give me your confidence, based on your internal knowledge, in populating the data given a SQL query and a condition to use to find the keys in the schema Give me a value of \"high\" or \"low\", where \"high\" is perfect confidence and \"low\" is no confidence. Return a \"high\" score if you can return accurate and factual data. Return a \"low\" if you can't return accurate and factual data or you may not have information on all the data. Return the results in JSON with the property \"confidence\"\n"
                + "\n"
                + "SQL query: ${sqlQuery}\n"
                + "Condition: ${conditions}\n"
                + "Keys: ${keys}\n";
        String relationalSchema = "";
        Set<String> keys = new HashSet<>();
        for (String tableName : tableNames) {
            ITable table = database.getTable(tableName);
            List<Attribute> attributes = table.getAttributes();
            String schema = getRelationalSchema(attributes,tableName);
            relationalSchema += schema + " ";
        }
        for (Key key : database.getKeys()) {
            for (AttributeRef attribute : key.getAttributes()) {
                String tableName = attribute.getTableName();
                if (tableNames.contains(tableName)) {
                    keys.add(attribute.getName());
                }
             }
        }
        relationalSchema = relationalSchema.trim();
        String promptTable = prompt.replace("${relationalSchema}", relationalSchema);
        promptTable = promptTable.replace("${sqlQuery}", querySQL);
        String keysName = keys.toString().replace("[", "").replace("]", "");
        promptTable = promptTable.replace("${keys}", keysName);
        ParserWhere parserWhere = new ParserWhere();
        parserWhere.parseWhere(querySQL);
        String whereExpression = parserWhere.getWhereExpression().trim();
        promptTable = promptTable.replace("${conditions}", whereExpression);
        if (log.isDebugEnabled()) log.debug("Request:\n {}", promptTable);
        ChatLanguageModel model = new TogetherAIModel(Constants.TOGETHERAI_API, togetherAIModelName, TogetherAIConstants.STREAM_MODE);
        if (Constants.LLM_MODEL.equals(Constants.MODEL_GPT)) model = ConversationalChainFactory.buildOpenAIChatLanguageModel(Constants.OPEN_AI_API_KEY, Constants.OPEN_AI_CHAT_MODEL_NAME);
        String response = model.generate(promptTable);   
        if (log.isDebugEnabled()) log.debug("Response:\n {}", response);
    }
    
    public Map<String, String> getEstimationForQuery2(IDatabase database, List<String> tableNames, String querySQL) {
//        String prompt1 = "Given the following schema:\n"
//                + "'''\n${relationalSchema}\n'''\n"
//                + "\n"
//                + "and the following conditions that hold over it:\n"
//                + "'''\n${conditions}\n'''\n"
//                + "\n"
//                + "What are the attributes in the conditions for which you are pretty sure about the values for ${attrs} your knowledge and what are the attributes in the conditions for which you are not sure at all using your knowledge?\n"
//                + "\n"
//                + "Consider that:\n"
//                + "- attributes that change their value over time are attributes with a lower level of confidence, while attributes that are static over time are attributes with a high level of confidence.\n"
//                + "- The confidence depends on the actual condition values. There are values for which in your knowledge you are most confident with respect to others who are not confident at all.";
        String prompt1 = "Here a way to estimate the confidence on a single condition or on multiple conditions:\n"
                + "1) In estimating the confidence of a condition you should consider how confider are in retrieve other factual values given that condition. Assign an high confidence to conditions where you are sure about the retrieving of other values for other attributes, assign a lower level of confidence otherwise;\n"
                + "2) In estimating the confidence of a condition consider the case where for some values in your knowledge your are highly confident with respect to other values in your knowledge;\n"
                + "3) In estimating the confidence of a condition you should consider that some attributes change their value over time, while other are static. Assign an high confidence to conditions that involve static attributes, assign a lower confidence to conditions that involve dynamic attributes;\n"
                + "\n"
                + "Given the following schema:\n"
                + "'''\n${relationalSchema}\n'''\n"
                + "\n"
                + "and the following conditions that hold over it:\n"
                + "'''\n${conditions}\n'''\n"
                + "\n"
                + "Estimate your confidence for the conditions. Return the confidence for each attribute. Use the value of \"high\" and \"low\" for the confidence.";
        


        String prompt2 = "Considering only the attributes in the condition ${conditionAttributes}, return a confidence for them in retrieving the values for the following attributes: ${attrs}.\n"
                + "Assign a confidence score based on previous observation, but in addition update your confidence considering that you should use those attribute values to find factual an real values for ${attrs}";
        
//        String promptJSON = "Return your answer in a single JSON format without any comment using the property \"attribute\" and \"confidence\". For high confidence return \"high\" for low confidence return \"low\".";
        String promptJSON = "Return your answer in a valid JSON format using for each attribute the property \"attribute\" and \"confidence\". Return the result as a JSON list. For confidence use the following values \"high\", \"medium\" or \"low\".";
        String promptJSONNoCode = "Do not return your answer with any code. " + promptJSON;
        String relationalSchema = "";
        for (String tableName : tableNames) {
            ITable table = database.getTable(tableName);
            List<Attribute> attributes = table.getAttributes();
            String schema = getRelationalSchema(attributes,tableName);
            relationalSchema += schema + " ";
        }
        relationalSchema = relationalSchema.trim();
        String promptTable = prompt1.replace("${relationalSchema}", relationalSchema);
        
        ParserWhere parserWhere = new ParserWhere();
        parserWhere.parseWhere(querySQL);
        String whereExpression = parserWhere.getWhereExpression().trim();
        promptTable = promptTable.replace("${conditions}", whereExpression);
        
        ParserProvenance parserProvenance = new ParserProvenance(database);
        parserProvenance.parse(querySQL);
        Set<String> attrs = parserProvenance.getAttributesInSelect();
        Set<String> attrsTotal = parserProvenance.getAttributeProvenance();
        String as = (attrs.toString().replace("[", "")).replace("]", "");
        String attrsString = "(" + as + ")";
        promptTable = promptTable.replace("${attrs}", attrsString);        
        if (log.isDebugEnabled()) log.debug("Prompt: \n {}", promptTable);
        ChatLanguageModel model = new TogetherAIModel(Constants.TOGETHERAI_API, togetherAIModelName, TogetherAIConstants.STREAM_MODE);
        if (Constants.LLM_MODEL.equals(Constants.MODEL_GPT)) model = ConversationalChainFactory.buildOpenAIChatLanguageModel(Constants.OPEN_AI_API_KEY, Constants.OPEN_AI_CHAT_MODEL_NAME);
        String response = model.generate(promptTable);
        if (log.isDebugEnabled()) log.debug("Response: \n {}", response);
        
//        prompt2 = prompt2.replace("${attrs}", attrsString);
//        attrsTotal.removeAll(attrs);
//        String as2 = (prompt2.toString().replace("[", "")).replace("]", "");
//        String attrsString2 = "(" + as2 + ")";
//        prompt2 = prompt2.replace("${conditionAttributes}", attrsString2);
//        prompt2 = response +"\n\n" + prompt2;
//        response = model.generate(prompt2);
//        if (log.isDebugEnabled()) log.debug(response);
        promptJSON = response + "\n\n" + promptJSON;
        if (log.isDebugEnabled()) log.debug("Prompt: \n {}", promptJSON);
        String responseJSON = model.generate(promptJSON);
        if (log.isDebugEnabled()) log.debug("Response: \n {}",responseJSON);
        String cleanedResponse = Mapper.toCleanJsonList(responseJSON, true);
        if (log.isDebugEnabled()) log.debug("Cleaned Response: {}", cleanedResponse);
        Map<String, String> confidencesForAttrsConditions = new HashMap<>();
        List<Map<String, Object>> confidences = null;
        try {
            confidences = Mapper.fromJsonToListOfMaps(cleanedResponse, false);
        } catch (Exception e) {
            log.error("Error in parsing the response for confidence.");
            responseJSON = model.generate(responseJSON + "\n" +promptJSONNoCode);
            if (log.isDebugEnabled()) log.debug("Response: \n {}",responseJSON);
            cleanedResponse = Mapper.toCleanJsonList(responseJSON, true);
            if (log.isDebugEnabled()) log.debug("Cleaned Response: {}", cleanedResponse);
            try {
                 confidences = Mapper.fromJsonToListOfMaps(cleanedResponse, false);
            } catch (Exception internal) {}
        }
        if (confidences == null) return confidencesForAttrsConditions;
        for (Map<String, Object> confidence : confidences) {
            String attribute = confidence.get("attribute").toString();
            String confidenceString = confidence.get("confidence").toString();
            if (whereExpression.contains(attribute)) {
                 if (log.isDebugEnabled()) log.debug("Attribute: {} - Confidence: {}", attribute, confidenceString);
                 confidencesForAttrsConditions.put(attribute, confidenceString);
            }
        }
        return confidencesForAttrsConditions;
    }
    
    public Double getEstimationConfidence(IDatabase database, List<String> tableNames, String querySQL, List<Expression> conditionsPushDown) {
        /*String prompt = "Given the following relational schema ${relationalSchema} please give me your confidence, based on your internal knowledge, in populating the data given a SQL query. Give me a value between 0 and 1, where 1 is perfect confidence and 0 is no confidence. Return a higher score if you are able to return accurate and factual data. Return a lower score if you can't return accurate and factual data or you may not have information on all the data. Return the results in JSON with the property \"confidence\".\n"
                + "\n"
                + "SQL query: ${sqlQuery}"; */
        String prompt1 = "Using your knowledge can you estimate the reliability and factuality of your answer in retrieving structured data from your knowledge, measuring it as a confidence value? Return a value between 0.0 and 1.0. Assign 0.0 when you are not confident at all in retrieving the requested data. Assign a score of 1.0 when you are confident about the requested data.\n"
                + "\n"
                + "I will prompt you the following information that you will use to estimate the confidence:\n"
                + "- The *json schema* of the data. Use it to understand the topic and measure how you are confident with the topic;\n"
                + "- The *set of attributes* for which I want to retrieve reliable and factual data;\n"
                + "- The *set of conditions* that you can use to retrieve specified values for the set of attributes;\n"
                + "- The *sql query* that we run over the retrieved data. Consider the difficulty of the query in estimating how the retrieved values for the *set of attributes* will help in getting reliable and factual responses.";
        
        String prompt2 = "*json schema*: ${relationalSchema}\n"
                + "*set of attributes*: ${keys}\n"
                + "*set of conditions*: ${conditions}\n"
                + "*sql query*: ${sqlQuery}";
        
        String promptResult = "Return the confidence value in JSON format with an attribute \"confidence\"";
        
        String prompt = prompt1 + "\n\n" + prompt2 + "\n\n" + promptResult;
        
        String relationalSchema = "";
        Set<String> keys = new HashSet<>();
        for (String tableName : tableNames) {
            ITable table = database.getTable(tableName);
            List<Attribute> attributes = table.getAttributes();
            String schema = getRelationalSchema(attributes,tableName);
            relationalSchema += schema + " ";
        }
        for (Key key : database.getKeys()) {
            for (AttributeRef attribute : key.getAttributes()) {
                String tableName = attribute.getTableName();
                if (tableNames.contains(tableName)) {
                    keys.add(attribute.getName());
                }
             }
        }
        relationalSchema = relationalSchema.trim();
        String promptTable = prompt.replace("${relationalSchema}", relationalSchema);
        promptTable = promptTable.replace("${sqlQuery}", querySQL);
        String keysName = keys.toString().replace("[", "").replace("]", "");
        promptTable = promptTable.replace("${keys}", keysName);
        String conditionExpression = "";
        if (!conditionsPushDown.isEmpty()) {
            conditionExpression = conditionsPushDown.toString().replace("[", "").replace("]", "");
        }
        promptTable = promptTable.replace("${conditions}", conditionExpression);
        if (log.isDebugEnabled()) log.debug("Request:\n {}", promptTable);
        ChatLanguageModel model = new TogetherAIModel(Constants.TOGETHERAI_API, togetherAIModelName, TogetherAIConstants.STREAM_MODE);
        if (Constants.LLM_MODEL.equals(Constants.MODEL_GPT)) model = ConversationalChainFactory.buildOpenAIChatLanguageModel(Constants.OPEN_AI_API_KEY, Constants.OPEN_AI_CHAT_MODEL_NAME);
        String response = model.generate(promptTable);
        if (log.isDebugEnabled()) log.debug("Response JSON:\n {}", response);
        String cleanedJson = Mapper.toCleanJsonObject(response);
        if (log.isDebugEnabled()) log.debug("JSON:\n {}", cleanedJson);
        Map<String, Object> jsonResponseObj = new HashMap<>();
        try {
            jsonResponseObj = Mapper.fromJsonToMap(cleanedJson);
        } catch (Exception e) {
            log.error("Error in parsing the response for confidence value.");
            response = model.generate(response + "\n" +promptResult);
            if (log.isDebugEnabled()) log.debug("Response JSON: \n {}",response);
            cleanedJson = Mapper.toCleanJsonObject(response);
            if (log.isDebugEnabled()) log.debug("Cleaned Response: {}", cleanedJson);
            try {
                 jsonResponseObj = Mapper.fromJsonToMap(cleanedJson);
            } catch (Exception internal) {}
        }
        for (String propName : jsonResponseObj.keySet()) {
            if (propName.equalsIgnoreCase("confidence")) {
                String sConf = jsonResponseObj.get(propName).toString();
                try {
                    return Double.valueOf(sConf.replace(",", "."));
                } catch (NumberFormatException nfe) {
                    
                }
            }
        }
        return null;
    }
    
    public Double getEstimationConfidence2(IDatabase database, List<String> tableNames, String querySQL, List<Expression> conditionsPushDown) {
        /*String prompt = "Given the following relational schema ${relationalSchema} please give me your confidence, based on your internal knowledge, in populating the data given a SQL query. Give me a value between 0 and 1, where 1 is perfect confidence and 0 is no confidence. Return a higher score if you are able to return accurate and factual data. Return a lower score if you can't return accurate and factual data or you may not have information on all the data. Return the results in JSON with the property \"confidence\".\n"
                + "\n"
                + "SQL query: ${sqlQuery}"; */
        String conditionsPrompt = "";
        if (conditionsPushDown != null && !conditionsPushDown.isEmpty()) {
            conditionsPrompt = " given the conditions: ${conditions}";
        }
        String prompt = "Using your knowledge sample (max 10 tuples) factual data for the given schema: ${relationalSchema}" + conditionsPrompt + ".\n"
                + "\n"
                + "Using the sampled data estimate your confidence in retrieving more values for the attribute: ${keys}.\n"
                + "In estimating the confidence consider also the complexity of the following query: ${sqlQuery}.\n"
                + "\n"
                + "Return a value between 0.0 and 1.0. Assign 0.0 when you are not confident at all in retrieving the requested data. Assign a score of 1.0 when you are confident about the requested ${keys}.\n"
                + "\n"
                + "Return the confidence value in JSON format with an attribute \"confidence\"";

           
        String relationalSchema = "";
        Set<String> keys = new HashSet<>();
        for (String tableName : tableNames) {
            ITable table = database.getTable(tableName);
            List<Attribute> attributes = table.getAttributes();
            String schema = getRelationalSchema(attributes,tableName);
            relationalSchema += schema + " ";
        }
        for (Key key : database.getKeys()) {
            for (AttributeRef attribute : key.getAttributes()) {
                String tableName = attribute.getTableName();
                if (tableNames.contains(tableName)) {
                    keys.add(attribute.getName());
                }
             }
        }
        relationalSchema = relationalSchema.trim();
        String promptTable = prompt.replace("${relationalSchema}", relationalSchema);
        promptTable = promptTable.replace("${sqlQuery}", querySQL);
        String keysName = keys.toString().replace("[", "").replace("]", "");
        promptTable = promptTable.replace("${keys}", keysName);
        String conditionExpression = "";
        if (!conditionsPushDown.isEmpty()) {
            conditionExpression = conditionsPushDown.toString().replace("[", "").replace("]", "");
        }
        promptTable = promptTable.replace("${conditions}", conditionExpression);
        if (log.isDebugEnabled()) log.debug("Request:\n {}", promptTable);
        ChatLanguageModel model = new TogetherAIModel(Constants.TOGETHERAI_API, togetherAIModelName, TogetherAIConstants.STREAM_MODE);
        if (Constants.LLM_MODEL.equals(Constants.MODEL_GPT)) model = ConversationalChainFactory.buildOpenAIChatLanguageModel(Constants.OPEN_AI_API_KEY, Constants.OPEN_AI_CHAT_MODEL_NAME);
        String response = model.generate(promptTable);
        if (log.isDebugEnabled()) log.debug("Response JSON:\n {}", response);
        String cleanedJson = Mapper.toCleanJsonObject(response);
        if (log.isDebugEnabled()) log.debug("JSON:\n {}", cleanedJson);
        Map<String, Object> jsonResponseObj = Mapper.fromJsonToMap(cleanedJson);
        for (String propName : jsonResponseObj.keySet()) {
            if (propName.equalsIgnoreCase("confidence")) {
                String sConf = jsonResponseObj.get(propName).toString();
                try {
                    return Double.valueOf(sConf.replace(",", "."));
                } catch (NumberFormatException nfe) {
                    
                }
            }
        }
        return null;
    }
    
    public void getCardinalityEstimationForQuery(IDatabase database, List<String> tableNames, String querySQL) {
        ParserWhere parserWhere = new ParserWhere();
        parserWhere.parseWhere(querySQL);
        if (parserWhere.getExpressions().isEmpty()) return;
        String prompt = "Assuming that you have the knowledge on the data of the full table(s) of ${tables} with the following schema: ${relationalSchema}."
                + "\nConsidering the following query: ${sqlQuery}. \n"
                + "What is your estimation about the cardinality (low, medium, high) of the following conditions:\n"
                + "\n${conditions}\n"
                + "Answer in JSON format returning each condition and the associated cardinality (low, medium, high). Do not report any comments.";
        String relationalSchema = "";
        String tables = "";
        for (String tableName : tableNames) {
            ITable table = database.getTable(tableName);
            List<Attribute> attributes = table.getAttributes();
            String schema = getRelationalSchema(attributes,tableName);
            relationalSchema += schema + " ";
            tables += tableName+ " ";
        }
        relationalSchema = relationalSchema.trim();
        tables = tables.trim();
        String conditions = "";
        String allConditions = parserWhere.getWhereExpression();
        if (parserWhere.getExpressions().size() > 1) {
            for (int i = 0; i < parserWhere.getExpressions().size(); i++) {
                conditions += parserWhere.getExpressions().get(i) + ";\n";
            }
        }
        conditions += allConditions+";\n";
        String promptTable = prompt.replace("${tables}", tables);
        promptTable = promptTable.replace("${relationalSchema}", relationalSchema);
        promptTable = promptTable.replace("${sqlQuery}", querySQL);
        promptTable = promptTable.replace("${conditions}", conditions);       
        if (log.isDebugEnabled()) log.debug(promptTable);
        ChatLanguageModel model = new TogetherAIModel(Constants.TOGETHERAI_API, togetherAIModelName, TogetherAIConstants.STREAM_MODE);
        if (Constants.LLM_MODEL.equals(Constants.MODEL_GPT)) model = ConversationalChainFactory.buildOpenAIChatLanguageModel(Constants.OPEN_AI_API_KEY, Constants.OPEN_AI_CHAT_MODEL_NAME);
        String response = model.generate(promptTable);   
        if (log.isDebugEnabled()) log.debug(response);
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
            ChatLanguageModel model = new TogetherAIModel(Constants.TOGETHERAI_API, togetherAIModelName, TogetherAIConstants.STREAM_MODE);
            if (Constants.LLM_MODEL.equals(Constants.MODEL_GPT)) model = ConversationalChainFactory.buildOpenAIChatLanguageModel(Constants.OPEN_AI_API_KEY, Constants.OPEN_AI_CHAT_MODEL_NAME);
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
