package galois.test.experiments.run;

import ai.djl.modality.nlp.embedding.TextEmbedding;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.HuggingFaceTokenizer;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.*;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import galois.Constants;
import galois.llm.algebra.LLMScan;
import galois.llm.database.LLMDB;
import galois.llm.models.TogetherAIEmbeddingModel;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.togetherai.llama3.TogetheraiLlama3NLQueryExecutor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.IDatabase;
import speedy.model.database.TableAlias;
import speedy.model.database.Tuple;
import speedy.persistence.relational.AccessConfiguration;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static galois.test.utils.TestUtils.toTupleStream;

@Slf4j
public class TestRAG {

    private static final String EXP_NAME = "BBC-PremierLeague2024-2025";
    private static final String EXT = ".MD";

//    private static final String TEXT_TO_CHECK = "Sinisterra";
//    private static final String NL_PROMPT = "Player of the match of Bournemouth";

    private static final String TEXT_TO_CHECK = "Nvidia";
//    private static final String NL_PROMPT = "select company, headquartersstate, ticker, ceo, founder_is_ceo, is_femaleceo, number_of_employees from fortune_2024 where headquarterscity = 'Santa Clara' and rank < 70";
    private static final String NL_PROMPT = "company == \"Nvidia\" ";

//    private static final String NL_PROMPT = "Given the following query, populate the table with actual values.\n" +
//            "query: select player_of_the_match from premier_league_2024_2025_match_result.\n" +
//            "{\"title\":\"premier_league_2024_2025_match_result\",\"type\":\"array\",\"items\":{\"type\":\"object\",\"properties\":{\"player_of_the_match\":{\"title\":\"player_of_the_match\",\"type\":\"string\"}}}} ";

//    private static final String NL_PROMPT = "Given the following query, populate the table with actual values.\n" +
//            "query: select player_of_the_match from premier_league_2024_2025_match_result.\n" +
//            "Respond with JSON only. Don't add any comment.\n" +
//            "Use the following JSON schema:\n" +
//            "{\"title\":\"premier_league_2024_2025_match_result\",\"type\":\"array\",\"items\":{\"type\":\"object\",\"properties\":{\"player_of_the_match\":{\"title\":\"player_of_the_match\",\"type\":\"string\"}}}} ";

    private IDatabase llmDB;
    private EmbeddingModel embeddingModel;
    private EmbeddingStoreIngestor embeddingStoreIngestor;
    private EmbeddingStore<TextSegment> embeddingStore;
    private ContentRetriever contentRetriever;
    private ExperimentConfiguration config;

    @BeforeEach
    public void setUp() {
        config = ExperimentConfiguration.builder()
                //Embedding
//                .embeddingModelEngine(EEmbeddingModelEngine.OLLAMA)
                .embeddingModelEngine(EEmbeddingModelEngine.T_AI)
//                .embeddingModel("mxbai-embed-large") //OLLAMA
                .embeddingModel("WhereIsAI/UAE-Large-V1") //T_AI
//                .embeddingModel("togethercomputer/m2-bert-80M-8k-retrieval") //T_AI
                //Ingestor
                .maxSegmentSizeInTokens(128)
                .maxOverlapSizeInTokens(64)
                .build();
        log.info("Experiment {}-{}", EXP_NAME, config.toString());
        configureDB();
        embeddingModel = buildEmbeddingModel();
        embeddingStore = buildEmbeddingStore();
        embeddingStoreIngestor = buildEmbeddingStoreIngestor();
        contentRetriever = buildContentRetriver();
        //
//        embeddingStore.removeAll();
//        loadDocuments();
    }

    @Test
    public void testEmbeddingStore() {
        Embedding queryEmbedding = embeddingModel.embed(NL_PROMPT).content();
        log.trace("Embedded query: {}", queryEmbedding);
        List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.findRelevant(queryEmbedding, 200);
        log.info("Relevant documents {}", relevant.size());
        Set<String> documents = new HashSet<>();
        int retrived = 0;
        for (int i = 0; i < relevant.size(); i++) {
            EmbeddingMatch<TextSegment> embeddingMatch = relevant.get(i);
            log.info("Result {}:\n\tScore: {}\n\tText: {}", i, embeddingMatch.score(), embeddingMatch.embedded().text());
            if (embeddingMatch.embedded().text().toUpperCase().contains(TEXT_TO_CHECK.toUpperCase())) retrived++;
            documents.add(embeddingMatch.embedded().metadata().getString("file_name"));
        }
        log.info("Configuration: {}\nDocuments with prompt: {}\nDifferent documents: {}", config, retrived, documents.size());
    }

    @Test
    public void testContentRetriever() {
        //Used to choose the best configuration
        // testEmbeddingStore vs testContentRetriever should give the same score
        List<Content> retrieved = contentRetriever.retrieve(Query.from(NL_PROMPT));
//        List<Content> retrieved = contentRetriever.retrieve(Query.from(TEXT_TO_CHECK));
        log.info("Relevant documents {}", retrieved.size());
        Set<String> documents = new HashSet<>();
        int retrived = 0;
        for (int i = 0; i < retrieved.size(); i++) {
            Content content = retrieved.get(i);
            log.info("Result {}:\n\tText: {}", i, content.textSegment().text());
            if (content.textSegment().text().toUpperCase().contains(TEXT_TO_CHECK.toUpperCase())) {
                retrived++;
//            if(content.textSegment().text().toUpperCase().contains(NL_PROMPT.toUpperCase())) retrived++;
                documents.add(content.textSegment().metadata().getString("file_name"));
            }
        }
        log.info("Configuration: {}\nDocuments with prompt: {}\nDifferent documents: {}", config, retrived, documents.size());
    }

    @Test
    public void exploreDataStore() {
        Embedding queryEmbedding = embeddingModel.embed("*").content();
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(Integer.MAX_VALUE)
                .minScore(0.0)
                .build());
        List<EmbeddingMatch<TextSegment>> documents = result.matches();
        log.info("Documents: {}", documents.size());
        for (EmbeddingMatch<TextSegment> document : documents) {
            log.info("{}",document.embedded());
        }
    }

    @Test
    public void executeNL() {
        IQueryExecutor executor = TogetheraiLlama3NLQueryExecutor.builder()
//        IQueryExecutor executor = OllamaLlama3NLQueryExecutor.builder()
                .naturalLanguagePrompt(NL_PROMPT)
                .contentRetriever(contentRetriever)
                .maxIterations(1)
                .build();
        testExecutor(executor);
    }

    private void testExecutor(IQueryExecutor executor) {
//        TableAlias tableAlias = new TableAlias("premier_league_2024_2025_player_of_the_match", "potm");
        TableAlias tableAlias = new TableAlias("premier_league_2024_2025_key_events", "scorers");
        IAlgebraOperator llmScan = new LLMScan(tableAlias, executor, null);
        ITupleIterator tuples = llmScan.execute(llmDB, null);
        toTupleStream(tuples).map(Tuple::toString).forEach(log::info);
    }

    private void configureDB() {
        AccessConfiguration accessConfiguration = new AccessConfiguration();
        String driver = "org.postgresql.Driver";
        String uri = "jdbc:postgresql:llm_rag_premierleague";
        String schemaName = "public";
        String username = "pguser";
        String password = "pguser";
        accessConfiguration.setDriver(driver);
        accessConfiguration.setUri(uri);
        accessConfiguration.setSchemaName(schemaName);
        accessConfiguration.setLogin(username);
        accessConfiguration.setPassword(password);
        llmDB = new LLMDB(accessConfiguration);
    }

    private EmbeddingModel buildEmbeddingModel() {
        if (config.embeddingModelEngine == EEmbeddingModelEngine.OLLAMA) {
            return OllamaEmbeddingModel.builder()
                    .baseUrl("http://127.0.0.1:11434")
                    .modelName(config.embeddingModel)
                    .build();
        } else if (config.embeddingModelEngine == EEmbeddingModelEngine.T_AI) {
            return TogetherAIEmbeddingModel.builder()
                    .toghetherAiAPI(Constants.TOGETHERAI_API)
                    .modelName(config.embeddingModel)
                    .build();
        }
        return null;
    }

    private EmbeddingStore<TextSegment> buildEmbeddingStore() {
//        return new InMemoryEmbeddingStore<>();
        /*
        pip install chromadb
        chroma run
         */
        EmbeddingStore<TextSegment> embeddingStore = ChromaEmbeddingStore
                .builder()
                .baseUrl("http://localhost:8000")
//                .collectionName("rag_premierleague_128_64_UAE-Large")
//                .collectionName("rag_premierleague_128_64_UAE-Large-PROCESSED")
//                .collectionName("rag_fortune_128_64_UAE-Large")
                .collectionName("rag_fortune_400_50_UAE-Large-PROCESSED")
//                .collectionName(EXP_NAME + "-" + config.toString().hashCode())
                .logRequests(true)
                .logResponses(true)
                .build();
        return embeddingStore;
    }

    private EmbeddingStoreIngestor buildEmbeddingStoreIngestor() {
        return EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(config.maxSegmentSizeInTokens, config.maxOverlapSizeInTokens, new HuggingFaceTokenizer()))
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

    private EmbeddingStoreContentRetriever buildContentRetriver() {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(50)
//                .dynamicMaxResults(query -> 3)
                .minScore(0.75)
//                .dynamicMinScore(query -> 0.75)
                .build();
    }

    private void loadDocuments() {
        String expPath = TestRAG.class.getResource("/rag/" + EXP_NAME).getFile();
        log.info("Experiment path {}", expPath);
        File folder = new File(expPath);
        Integer MAX_FILES = null;
        int filesIndexed = 0;
        for (File file : folder.listFiles()) {
            if (file.isDirectory() || !file.getName().toUpperCase().endsWith(EXT)) continue;
//            if (!file.getName().toUpperCase().contains("MANUAL")) {
//                //TO USE MANUAL VERSION ONLY
//                continue;
//            }
            if (file.getName().toUpperCase().contains("MANUAL")) {
                //TO SKIP MANUAL VERSION
                continue;
            }
            log.info("Reading file {}", file.getName());
            Document document = FileSystemDocumentLoader.loadDocument(file.getPath());
            log.info("Document Metadata {}", document.metadata());
            embeddingStoreIngestor.ingest(document);

            filesIndexed++;
            if (MAX_FILES != null && filesIndexed == MAX_FILES) {
                log.warn("Loaded only {} files", filesIndexed);
                break;
            }
        }
    }

    @Builder
    static
    class ExperimentConfiguration {
        EEmbeddingModelEngine embeddingModelEngine;
        String embeddingModel;
        int maxSegmentSizeInTokens;
        int maxOverlapSizeInTokens;

        public String toString() {
            return "E" + embeddingModelEngine.toString() + "-" + embeddingModel + "-S" + maxSegmentSizeInTokens + "_" + maxOverlapSizeInTokens;
        }
    }

    enum EEmbeddingModelEngine {
        T_AI,
        OLLAMA
    }

}
