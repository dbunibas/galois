package galois.test.experiments.json.parser.operators;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.HuggingFaceTokenizer;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import galois.Constants;
import galois.llm.algebra.config.ScanConfiguration;
import galois.llm.models.TogetherAIEmbeddingModel;
import galois.llm.query.INLQueryExectutor;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.ollama.llama3.*;
import galois.llm.query.ollama.mistral.OllamaMistralNLQueryExecutor;
import galois.llm.query.togetherai.llama3.*;
import galois.prompt.EPrompts;
import galois.test.experiments.Query;
import galois.test.experiments.json.config.ContentRetrieverConfigurationJSON;
import galois.test.experiments.json.config.ScanConfigurationJSON;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class ScanConfigurationParser {
    private static final Map<String, IQueryExecutorGenerator> parserMap = Map.ofEntries(
            // Ollama - Llama3
            Map.entry("ollama-llama3-nl", ScanConfigurationParser::generateOllamaLlama3NLQueryExecutor),
            Map.entry("ollama-llama3-sql", ScanConfigurationParser::generateOllamaLlama3SQLQueryExecutor),
            Map.entry("ollama-llama3-table", ScanConfigurationParser::generateOllamaLlama3TableQueryExecutor),
            Map.entry("ollama-llama3-key", ScanConfigurationParser::generateOllamaLlama3KeyQueryExecutor),
            Map.entry("ollama-llama3-key-scan", ScanConfigurationParser::generateOllamaLlama3KeyScanQueryExecutor),
            Map.entry("togetherai-llama3-nl", ScanConfigurationParser::generateTogetheraiLlama3NLQueryExecutor),
            Map.entry("togetherai-llama3-sql", ScanConfigurationParser::generateTogetheraiLlama3SQLQueryExecutor),
            Map.entry("togetherai-llama3-table", ScanConfigurationParser::generateTogetheraiLlama3TableQueryExecutor),
            Map.entry("togetherai-llama3-key", ScanConfigurationParser::generateTogetheraiLlama3KeyQueryExecutor),
            Map.entry("togetherai-llama3-key-scan", ScanConfigurationParser::generateTogetheraiLlama3KeyScanQueryExecutor),
            Map.entry("ollama-mistral-nl", ScanConfigurationParser::generateOllamaMistralNLQueryExecutor)
    );

    public static ScanConfiguration parse(ScanConfigurationJSON json, Query query) {
        IQueryExecutorGenerator generator = getExecutor(json, query);
        ScanConfiguration.IQueryExecutorFactory factory = (IQueryExecutor base) -> {
            String naturalLanguagePrompt = json.getNaturalLanguagePrompt();
            // Override nl prompt if base is not null - TestRunner .execute set it before
            if (base instanceof INLQueryExectutor nlQueryExecutor) {
                naturalLanguagePrompt = nlQueryExecutor.getNaturalLanguagePrompt();
            }
            ContentRetriever contentRetriever = loadContentRetriever(json.getContentRetriever());
            return generator.create(
                    json.getFirstPrompt(),
                    json.getIterativePrompt(),
                    json.getMaxIterations(),
                    json.getAttributesPrompt(),
                    naturalLanguagePrompt,
                    // Always use query sql - TestRunner .execute set it before
                    query.getSql(),
                    contentRetriever
            );
        };
        return new ScanConfiguration(factory.create(null), factory, json.getNormalizationStrategy());
    }

    private static ContentRetriever loadContentRetriever(ContentRetrieverConfigurationJSON contentRetrieverConf) {
        if (contentRetrieverConf == null) {
            return null;
        }
        EmbeddingModel embeddingModel = buildEmbeddingModel(contentRetrieverConf);
        EmbeddingStore<TextSegment> embeddingStore = buildEmbeddingStore(contentRetrieverConf);
        EmbeddingStoreIngestor embeddingStoreIngestor = buildEmbeddingStoreIngestor(contentRetrieverConf, embeddingModel, embeddingStore);
        ContentRetriever contentRetriever = buildContentRetriver(contentRetrieverConf, embeddingModel, embeddingStore);
        //TODO: TOGGLE TO IMPORT DATA
//        embeddingStore.removeAll();
//        loadDocuments(contentRetrieverConf, embeddingStoreIngestor);
        return contentRetriever;
    }

    private static EmbeddingModel buildEmbeddingModel(ContentRetrieverConfigurationJSON contentRetrieverConf) {
        if (contentRetrieverConf.getEmbeddingModelEngine().equalsIgnoreCase("ollama")) {
            return OllamaEmbeddingModel.builder()
                    .baseUrl("http://127.0.0.1:11434")
                    .modelName(contentRetrieverConf.getEmbeddingModel())
                    .build();
        } else if (contentRetrieverConf.getEmbeddingModelEngine().equalsIgnoreCase("togetherai")) {
            return TogetherAIEmbeddingModel.builder()
                    .toghetherAiAPI(Constants.TOGETHERAI_API)
                    .modelName(contentRetrieverConf.getEmbeddingModel())
                    .build();
        }
        throw new IllegalArgumentException("Unknown EmbeddingModelEngine " + contentRetrieverConf.getEmbeddingModelEngine());
    }

    private static EmbeddingStore<TextSegment> buildEmbeddingStore(ContentRetrieverConfigurationJSON contentRetrieverConf) {
        EmbeddingStore<TextSegment> embeddingStore = ChromaEmbeddingStore
                .builder()
                .baseUrl("http://localhost:8000")
                .collectionName(contentRetrieverConf.getEmbeddingStoreCollectionName())
                .logRequests(true)
                .logResponses(true)
                .build();
        return embeddingStore;
    }

    private static EmbeddingStoreIngestor buildEmbeddingStoreIngestor(ContentRetrieverConfigurationJSON contentRetrieverConf,
                                                                      EmbeddingModel embeddingModel,
                                                                      EmbeddingStore<TextSegment> embeddingStore) {
        return EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(contentRetrieverConf.getMaxSegmentSizeInTokens(),
                        contentRetrieverConf.getMaxOverlapSizeInTokens(),
                        new HuggingFaceTokenizer()))
//                .documentSplitter(new DocumentByParagraphSplitter(512, 128))
                .documentTransformer(document -> {
                    document.metadata().put("ingested_data", LocalDateTime.now().toString());
                    return document;
                })
                .textSegmentTransformer(textSegment -> TextSegment.from(
                        textSegment.metadata("file_name") + "\n" + textSegment.text(),
                        textSegment.metadata()
                ))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
    }

    private static EmbeddingStoreContentRetriever buildContentRetriver(ContentRetrieverConfigurationJSON contentRetrieverConf,
                                                                       EmbeddingModel embeddingModel,
                                                                       EmbeddingStore<TextSegment> embeddingStore) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(200)
                .minScore(0.75)
                .build();
    }

    private static void loadDocuments(ContentRetrieverConfigurationJSON contentRetrieverConf, EmbeddingStoreIngestor embeddingStoreIngestor) {
        File folder = new File(contentRetrieverConf.getDocumentsToLoad());
        for (File file : folder.listFiles()) {
            if (file.isDirectory() || file.isHidden()) continue;
            log.info("Reading file {}", file.getName());
            Document document = FileSystemDocumentLoader.loadDocument(file.getPath());
            log.info("Document Metadata {}", document.metadata());
            embeddingStoreIngestor.ingest(document);
        }
    }

    private static IQueryExecutorGenerator getExecutor(ScanConfigurationJSON json, Query query) {
        String name = json.getQueryExecutor();
        if (name == null || name.isEmpty() || !parserMap.containsKey(name))
            throw new IllegalArgumentException("Invalid executor name: " + name + "!");
        return parserMap.get(name);
    }

    private static Map<String, EPrompts> computePromptsMap() {
        return Arrays.stream(EPrompts.values()).collect(Collectors.toMap(EPrompts::name, Function.identity()));
    }

    private static IQueryExecutor generateOllamaLlama3NLQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OllamaLlama3NLQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, prompt, contentRetriever);
    }

    private static IQueryExecutor generateTogetheraiLlama3NLQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new TogetheraiLlama3NLQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, prompt, contentRetriever);
    }

    private static IQueryExecutor generateOllamaLlama3SQLQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OllamaLlama3SQLQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, sql, contentRetriever);
    }

    private static IQueryExecutor generateTogetheraiLlama3SQLQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new TogetheraiLlama3SQLQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, sql, contentRetriever);
    }

    private static IQueryExecutor generateOllamaLlama3TableQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OllamaLlama3TableQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, null, contentRetriever);
    }

    private static IQueryExecutor generateTogetheraiLlama3TableQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new TogetheraiLlama3TableQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, null, contentRetriever);
    }

    private static IQueryExecutor generateOllamaLlama3KeyQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OllamaLlama3KeyQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), promptsMap.get(attributesPrompt), maxIterations > 0 ? maxIterations : 10, null, contentRetriever);
    }

    private static IQueryExecutor generateTogetheraiLlama3KeyQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new TogetheraiLLama3KeyQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), promptsMap.get(attributesPrompt), maxIterations > 0 ? maxIterations : 10, null, contentRetriever);
    }

    private static IQueryExecutor generateOllamaLlama3KeyScanQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OllamaLlama3KeyScanQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), promptsMap.get(attributesPrompt), maxIterations > 0 ? maxIterations : 10, null, contentRetriever);
    }

    private static IQueryExecutor generateTogetheraiLlama3KeyScanQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new TogetheraiLlama3KeyScanQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), promptsMap.get(attributesPrompt), maxIterations > 0 ? maxIterations : 10, null, contentRetriever);
    }

    private static IQueryExecutor generateOllamaMistralNLQueryExecutor(String firstPrompt, String iterativePrompt, int maxIterations, String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever) {
        Map<String, EPrompts> promptsMap = computePromptsMap();
        return new OllamaMistralNLQueryExecutor(promptsMap.get(firstPrompt), promptsMap.get(iterativePrompt), maxIterations > 0 ? maxIterations : 10, prompt); //TODO: Handle content retriever
    }

    @FunctionalInterface
    private interface IQueryExecutorGenerator {
        IQueryExecutor create(String firstPrompt, String iterativePrompt, int maxIterations,
                              String attributesPrompt, String prompt, String sql, ContentRetriever contentRetriever);
    }
}
