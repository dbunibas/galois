package galois.llm.query.outlines;

import galois.llm.query.IQueryExecutor;
import galois.llm.query.http.payloads.JSONPayload;
import galois.llm.query.http.payloads.RegexPayload;
import galois.llm.query.http.services.OutlinesService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import speedy.SpeedyConstants;
import speedy.model.database.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static galois.llm.query.QueryUtils.*;
import static galois.llm.query.http.HTTPUtils.executeSyncRequest;
import static galois.utils.Mapper.fromJSON;

@Slf4j
public class OutlinesKeyAttributesQueryExecutor implements IQueryExecutor {
    // private static final String MODEL_NAME = "mistral-7b-instruct-v0.2.Q4_K_M.gguf";
    private static final String MODEL_NAME = "Meta-Llama-3-8B-Instruct.Q4_K_M.gguf";

    private final OutlinesService outlinesService;

    private final int maxKeyIterations = 10;
    private int currentKeyIteration = 0;

    public OutlinesKeyAttributesQueryExecutor() {
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(5, TimeUnit.MINUTES)
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
        Set<String> keyValues = generateKeyValues(table, primaryKey);
        log.debug("keyValues: {}", keyValues);

        return keyValues.stream()
                .map(k -> generateTupleFromKey(k, table, tableAlias, primaryKeyAttributes))
                .toList();
    }

    private Set<String> generateKeyValues(ITable table, Key primaryKey) {
        // TODO: Check composite primary key
        String key = primaryKey.getAttributes().stream()
                .map(AttributeRef::getName)
                .collect(Collectors.joining(" and "));

        Set<String> keys = Set.of();
        Set<String> lastKeys = Set.of();

        String keyRegex = generateRegexForKeys();

        while (currentKeyIteration < maxKeyIterations) {
            String prompt = currentKeyIteration == 0 ?
                    getKeyPrompt(table, key) :
                    getIterativeKeyPrompt(table, key, lastKeys);

            RegexPayload payload = new RegexPayload(MODEL_NAME, prompt, keyRegex);
            Call<String> call = outlinesService.regex(payload);
            // TODO: Delete replaceAll?
            String response = executeSyncRequest(call).replaceAll("\"", "");
            lastKeys = Arrays.stream(response.split(",")).map(String::trim).collect(Collectors.toUnmodifiableSet());
            keys = Stream.concat(keys.stream(), lastKeys.stream()).collect(Collectors.toUnmodifiableSet());

            currentKeyIteration++;
        }

        return keys;
    }

    private Tuple generateTupleFromKey(String key, ITable table, TableAlias tableAlias, List<String> primaryKeyAttributes) {
        Tuple tuple = createNewTupleWithMockOID(tableAlias);
        addCellForPrimaryKey(key, tableAlias, tuple, primaryKeyAttributes);

        List<Attribute> attributes = table.getAttributes().stream()
                .filter(a -> !a.getName().equals("oid") && !primaryKeyAttributes.contains(a.getName()))
                .toList();
        addValueFromAttributes(table, tableAlias, attributes, tuple, key);

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

    private void addValueFromAttributes(ITable table, TableAlias tableAlias, List<Attribute> attributes, Tuple tuple, String key) {
        String prompt = getAttributesPrompt(table, attributes, key);
        String schema = generateJsonSchemaFromAttributes(table, attributes);
        JSONPayload payload = new JSONPayload(MODEL_NAME, prompt, schema);

        Call<String> call = outlinesService.json(payload);
        String response = executeSyncRequest(call);
        Map<String, Object> jsonMap = fromJSON(response);

        for (Attribute attribute : attributes) {
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
    }

    private String getKeyPrompt(ITable table, String key) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("List the ").append(key);
        prompt.append(" of some ").append(table.getName()).append("s. ");
        prompt.append("Just report the values in a row separated by commas without any comments.");

        log.debug("Keys prompt is: {}", prompt);

        return prompt.toString();
    }

    private String getIterativeKeyPrompt(ITable table, String key, Collection<String> values) {
        StringBuilder prompt = new StringBuilder();

//        prompt.append("Given the values: ").append(String.join(", ", values)).append("\n");
        prompt.append("Exclude the values: ").append(String.join(", ", values)).append(".\n");
//        prompt.append("Avoid repeating the values: ").append(String.join(", ", values)).append("\n");
        prompt.append("List the ").append(key);
        prompt.append(" of some other ").append(table.getName()).append("s. ");
//        prompt.append("Just report the values in a row separated by commas without any comments between ().");
        prompt.append("Just report the values in a row separated by commas without any comments.");

        log.debug("Iterative key prompt is: {}", prompt);

        return prompt.toString();
    }

    private String getAttributesPrompt(ITable table, List<Attribute> attributes, String key) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("List the ");
        prompt.append(attributes.stream().map(Attribute::getName).collect(Collectors.joining(" and ")));
        prompt.append(" of the ").append(table.getName()).append(" ").append(key).append(".\n");
        prompt.append("Just report the values in a row without any additional comments.");

        log.debug("Attribute prompt is: {}", prompt);

        return prompt.toString();
    }

}
