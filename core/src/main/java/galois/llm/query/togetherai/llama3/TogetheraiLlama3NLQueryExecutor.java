package galois.llm.query.togetherai.llama3;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import galois.Constants;
import galois.llm.query.*;
import galois.prompt.EPrompts;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.Attribute;
import speedy.model.database.ITable;

import java.util.List;

import static galois.llm.query.ConversationalChainFactory.buildTogetherAIConversationalChain;
import galois.llm.query.utils.QueryUtils;

import static galois.llm.query.ConversationalRetrievalChainFactory.buildTogetherAIConversationalRetrivalChain;
import static galois.llm.query.utils.QueryUtils.generateJsonSchemaListFromAttributes;
import static galois.llm.query.utils.QueryUtils.getCleanAttributes;
import static galois.utils.FunctionalUtils.orElse;
import galois.utils.GaloisDebug;
import java.util.ArrayList;
import java.util.Map;
import speedy.model.database.AttributeRef;
import speedy.model.database.IDatabase;
import speedy.model.database.TableAlias;
import speedy.model.database.Tuple;
import speedy.model.expressions.Expression;

@Slf4j
@Getter
@Setter
public class TogetheraiLlama3NLQueryExecutor extends AbstractEntityQueryExecutor implements INLQueryExectutor {

    private final EPrompts firstPrompt;
    private final EPrompts iterativePrompt;
    private final int maxIterations;
    private String naturalLanguagePrompt;
    private final ContentRetriever contentRetriever;

    public TogetheraiLlama3NLQueryExecutor(String naturalLanguagePrompt) {
        this.firstPrompt = EPrompts.NATURAL_LANGUAGE_JSON;
        this.iterativePrompt = EPrompts.LIST_DIFFERENT_VALUES_JSON;
        this.maxIterations = 10;
        this.naturalLanguagePrompt = naturalLanguagePrompt;
        this.contentRetriever = null;
    }

    public TogetheraiLlama3NLQueryExecutor(
            EPrompts firstPrompt,
            EPrompts iterativePrompt,
            Integer maxIterations,
            String naturalLanguagePrompt, ContentRetriever contentRetriever
    ) {
        this.firstPrompt = orElse(firstPrompt, EPrompts.NATURAL_LANGUAGE_JSON);
        this.iterativePrompt = orElse(iterativePrompt, EPrompts.LIST_DIFFERENT_VALUES_JSON);
        this.maxIterations = maxIterations;
        this.contentRetriever = contentRetriever;
        if (naturalLanguagePrompt == null || naturalLanguagePrompt.isBlank()) {
            throw new IllegalArgumentException("naturalLanguagePrompt cannot be null or blank!");
        }
        this.naturalLanguagePrompt = naturalLanguagePrompt;
    }

    @Override
    public boolean ignoreTree() {
        return true;
    }

    @Override
    public List<Tuple> execute(IDatabase database, TableAlias tableAlias) {
        Chain<String, String> chain = getConversationalChain();
        ITable table = database.getTable(tableAlias.getTableName());
        List<Attribute> attributesExecution = getCleanAttributes(table);
        if (this.attributes != null && !this.attributes.isEmpty()) {
            attributesExecution = new ArrayList<>();
            for (AttributeRef attribute : this.attributes) {
                attributesExecution.add(table.getAttribute(attribute.getName()));
            }
        }
        String jsonSchema = generateJsonSchemaListFromAttributes(table, attributesExecution);
        List<Tuple> tuples = new ArrayList<>();
        for (int i = 0; i < getMaxIterations(); i++) {
            String userMessage = i == 0
                    ? generateFirstPrompt(table, attributesExecution,null, jsonSchema)
                    : generateIterativePrompt(table, attributesExecution, jsonSchema);
            log.debug("Prompt is: {}", userMessage);
            try {
                String response = super.getResponse(chain, userMessage, false);
                log.debug("First Response is: {}", response);
                List<Map<String, Object>> parsedResponse = getFirstPrompt().getEntitiesParser().parse(response, table);
                log.debug("First Parsed response is: {}", parsedResponse);
                if (parsedResponse.isEmpty()) {
                    break; // no more iterations
                }
                for (Map<String, Object> map : parsedResponse) {
                    log.debug("Try to parse: " + speedy.utility.SpeedyUtility.printMapCompact(map));
                    Tuple tuple = QueryUtils.mapToTupleIgnoreMissingAttributes(map, tableAlias);
                    // TODO: Handle possible duplicates
                    if (tuple != null ) {
                        log.debug("Add tuple: " + tuple);
                        tuples.add(tuple);
                    }
                }
            } catch (Exception e) {
                try {
                    log.debug("Error with the response, try again with attention on JSON format");
                    String response = getResponse(chain, EPrompts.ERROR_JSON_FORMAT.getTemplate(), true);
                    log.debug("Second Response is: {}", response);
                    List<Map<String, Object>> parsedResponse = getFirstPrompt().getEntitiesParser().parse(response, table);
                    log.debug("Second Parsed response is: {}", parsedResponse);
                    for (Map<String, Object> map : parsedResponse) {
                        log.debug("Second Try to parse: " + speedy.utility.SpeedyUtility.printMapCompact(map));
                        Tuple tuple = QueryUtils.mapToTupleIgnoreMissingAttributes(map, tableAlias);
                        // TODO: Handle possible duplicates
                        if (tuple != null) {
                           log.debug("Add tuple: " + tuple);
                            tuples.add(tuple);
                        }
                    }
                } catch (Exception internal) {
                    // do nothing
                }
            }
        }
        GaloisDebug.log("LLMScan results:");
        GaloisDebug.log(tuples);
        return tuples;
    }

    @Override
    protected Chain<String, String> getConversationalChain() {
        if(contentRetriever == null) {
            return buildTogetherAIConversationalChain(Constants.TOGETHERAI_API, Constants.TOGETHERAI_MODEL);
        }else {
            return buildTogetherAIConversationalRetrivalChain(Constants.TOGETHERAI_API, Constants.TOGETHERAI_MODEL, contentRetriever);
        }
    }

    @Override
    protected String generateFirstPrompt(ITable table, List<Attribute> attributes, Expression expression, String jsonSchema) {
        return firstPrompt.generateUsingNL(naturalLanguagePrompt, jsonSchema);
    }

    public static TogetheraiLlama3NLQueryExecutorBuilder builder() {
        return new TogetheraiLlama3NLQueryExecutorBuilder();
    }

    @Override
    public IQueryExecutorBuilder getBuilder() {
        return builder();
    }

    public static class TogetheraiLlama3NLQueryExecutorBuilder extends AbstractQueryExecutorBuilder {

        private String naturalLanguagePrompt;

        public TogetheraiLlama3NLQueryExecutorBuilder naturalLanguagePrompt(String naturalLanguagePrompt) {
            this.naturalLanguagePrompt = naturalLanguagePrompt;
            return this;
        }

        @Override
        public IQueryExecutor build() {
            return new TogetheraiLlama3NLQueryExecutor(
                    getFirstPrompt(),
                    getIterativePrompt(),
                    getMaxIterations(),
                    naturalLanguagePrompt,
                    getContentRetriever()
            );
        }
    }
}
