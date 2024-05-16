package galois.llm.query.outlines;

import galois.llm.models.IModel;
import galois.llm.models.OutlinesModel;
import galois.llm.query.IQueryExecutor;
import galois.http.payloads.JSONPayload;
import galois.http.payloads.TextPayload;
import galois.http.services.OutlinesService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import speedy.SpeedyConstants;
import speedy.model.database.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static galois.llm.query.QueryUtils.createNewTupleWithMockOID;
import static galois.llm.query.QueryUtils.generateJsonSchemaFromAttribute;
import static galois.http.HTTPUtils.executeSyncRequest;
import static galois.utils.Mapper.fromJSON;

@Slf4j
public class OutlinesKeyValueQueryExecutor implements IQueryExecutor {
    // private static final String MODEL_NAME = "mistral-7b-instruct-v0.2.Q4_K_M.gguf";
    private static final String MODEL_NAME = "Meta-Llama-3-8B-Instruct.Q4_K_M.gguf";

    private final IModel model;

    public OutlinesKeyValueQueryExecutor() {
        this.model = new OutlinesModel(MODEL_NAME);
    }

    @Override
    public List<Tuple> execute(IDatabase database, TableAlias tableAlias) {
        ITable table = database.getTable(tableAlias.getTableName());
        Key primaryKey = database.getPrimaryKey(table.getName());
        List<String> primaryKeyAttributes = primaryKey.getAttributes().stream()
                .map(AttributeRef::getName)
                .toList();

        // TODO: Add http retry mechanism?
        List<String> keyValues = generateKeyValues(table, primaryKey);
        log.debug("keyValues: {}", keyValues);

        // TODO: Remove!!
        return keyValues.subList(0, 2).stream()
                .map(k -> generateTupleFromKey(k, table, tableAlias, primaryKeyAttributes))
                .toList();
    }

    private List<String> generateKeyValues(ITable table, Key primaryKey) {
        String prompt = getKeyPrompt(table, primaryKey);
        String response = model.text(prompt);
        return Arrays.stream(response.split(",")).map(String::trim).toList();
    }

    private Tuple generateTupleFromKey(String key, ITable table, TableAlias tableAlias, List<String> primaryKeyAttributes) {
        Tuple tuple = createNewTupleWithMockOID(tableAlias);
        addCellForPrimaryKey(key, tableAlias, tuple, primaryKeyAttributes);

        List<Attribute> attributes = table.getAttributes().stream()
                .filter(a -> !a.getName().equals("oid") && !primaryKeyAttributes.contains(a.getName()))
                .toList();
        attributes.forEach(a -> addValueFromAttribute(table, tableAlias, a, tuple, key));

        return tuple;
    }

    private void addCellForPrimaryKey(String key, TableAlias tableAlias, Tuple tuple, List<String> primaryKeyAttributes) {
        // TODO: Handle composite key
        Cell keyCell = new Cell(
                tuple.getOid(),
                new AttributeRef(tableAlias, primaryKeyAttributes.get(0)),
                new ConstantValue(key)
        );
        tuple.addCell(keyCell);
    }

    private void addValueFromAttribute(ITable table, TableAlias tableAlias, Attribute attribute, Tuple tuple, String key) {
        String prompt = getAttributePrompt(table, attribute, key);
        String schema = generateJsonSchemaFromAttribute(table, attribute);
        Map<String, Object> jsonMap = model.json(prompt, schema);

        IValue cellValue = jsonMap.containsKey(attribute.getName()) ?
                new ConstantValue(jsonMap.get(attribute.getName())) :
                new NullValue(SpeedyConstants.NULL_VALUE);
        Cell currentCell = new Cell(
                tuple.getOid(),
                new AttributeRef(tableAlias, attribute.getName()),
                cellValue
        );

        tuple.addCell(currentCell);
    }

    private String getKeyPrompt(ITable table, Key primaryKey) {
        StringBuilder prompt = new StringBuilder();

        String key = primaryKey.getAttributes().stream()
                .map(AttributeRef::getName)
                .collect(Collectors.joining(" and "));

        prompt.append("List the ").append(key);
        prompt.append(" of some ").append(table.getName()).append("s. ");
        prompt.append("Just report the values in a row separated by commas without any comments.");

        log.debug("Keys prompt is: {}", prompt);

        return prompt.toString();
    }

    private String getAttributePrompt(ITable table, Attribute attribute, String key) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("List the ");
        prompt.append(attribute.getName());
        prompt.append(" of the ").append(table.getName()).append(" ").append(key).append(".\n");
        prompt.append("Just report the value without any additional comments.");

        log.debug("Attribute prompt is: {}", prompt);

        return prompt.toString();
    }

}
