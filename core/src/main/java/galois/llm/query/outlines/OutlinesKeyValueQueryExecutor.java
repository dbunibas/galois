package galois.llm.query.outlines;

import galois.llm.query.IQueryExecutor;
import galois.llm.query.http.payloads.JSONPayload;
import galois.llm.query.http.payloads.TextPayload;
import galois.llm.query.http.services.OutlinesService;
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
import static galois.llm.query.http.HTTPUtils.executeSyncRequest;
import static galois.utils.Mapper.fromJSON;

@Slf4j
public class OutlinesKeyValueQueryExecutor implements IQueryExecutor {
    OutlinesService outlinesService;

    public OutlinesKeyValueQueryExecutor() {
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://127.0.0.1:8000/")
                .client(client)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
        outlinesService = retrofit.create(OutlinesService.class);
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
        TextPayload payload = new TextPayload(getKeyPrompt(table, primaryKey));

        Call<String> call = outlinesService.text(payload);
        // TODO: Delete replaceAll?
        String response = executeSyncRequest(call).replaceAll("\"", "");

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
        JSONPayload payload = new JSONPayload(prompt, schema);

        Call<String> call = outlinesService.json(payload);
        String response = executeSyncRequest(call);
        Map<String, Object> map = fromJSON(response);

        IValue cellValue = map.containsKey(attribute.getName()) ?
                new ConstantValue(map.get(attribute.getName())) :
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
